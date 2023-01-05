/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.tools.analyzer.openjdk;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Main {
    private static final String DEFAULT_JMOD = "jmods/java.base.jmod";
    private static final String DEFAULT_JMOD_PATH = pathFromEnvOrThrow("ANDROID_JAVA_HOME")
            .resolve(DEFAULT_JMOD).toFile().getAbsolutePath();

    private static class MainArgs {

        @Parameter(names = "-h", help = true, description = "help usage")
        public boolean help = false;
    }

    @Parameters(commandNames = CommandDump.NAME, commandDescription = "Dump a class from the "
            + "classpath. This tool is similar to javap tool provided in the OpenJDK.")
    private static class CommandDump {
        public static final String NAME = "dump";

        @Parameter(names = {"-cp", "--classpath"},
                description = "file path to a .jmod or .jar file  which contains .class files.")
        public String classpathFile = DEFAULT_JMOD_PATH;

        @Parameter(required = true, arity = 1,
                description = "<class>. The fully qualified name of the class in the classpath. "
                        + "Note that inner class uses $ separator and $ has to be escaped in shell,"
                        + " e.g. java.lang.Character\\$Subset")
        public List<String> classNames;

        @Parameter(names = "-h", help = true, description = "help usage")
        public boolean help = false;

        private void run() throws UncheckedIOException {
            Path jmod = Path.of(classpathFile);
            String className = classNames.get(0);
            try (ZipFile zipFile = new ZipFile(jmod.toFile())) {
                ZipEntry e = getEntryFromClassNameOrThrow(zipFile, jmod, className);
                try (InputStream in = zipFile.getInputStream(e)) {
                    ClassReader classReader = new ClassReader(in);
                    PrintWriter printer = new PrintWriter(System.out);
                    TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printer);
                    classReader.accept(traceClassVisitor, 0);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Parameters(commandNames = CommandApiDiff.NAME, commandDescription = "List the new and "
            + "removed APIs by comparing the older and newer implementation in 2 jar/jmod files")
    private static class CommandApiDiff {
        public static final String NAME = "api-diff";

        @Parameter(names = {"-b", "--base"}, description = "file path to a .jmod or .jar file or "
                + "one of the following java version: oj, 8, 9, 11, 17")
        public String baseClasspath = "11";

        @Parameter(names = {"-n", "--new"}, description = "file path to a .jmod or .jar file or "
                + "one of the following java version: oj, 8, 9, 11, 17")
        public String newClasspath = "17";

        @Parameter(required = true, arity = 1,
                description = "<class>. The fully qualified name of the class in the classpath.\n"
                        + "Note that inner class uses $ separator and $ has to be escaped in shell,"
                        + " e.g. java.lang.Character\\$Subset")
        public List<String> classNames;

        @Parameter(names = "-h", help = true, description = "help usage")
        public boolean help = false;

        private void run() throws UncheckedIOException {
            Path basePath = toFilePath(baseClasspath);
            Path newPath = toFilePath(newClasspath);
            String className = classNames.get(0);
            DiffAnalyzer analyzer;
            try (ZipFile baseZip = new ZipFile(basePath.toFile());
                 ZipFile newZip = new ZipFile(newPath.toFile())) {
                ZipEntry baseEntry = getEntryFromClassNameOrThrow(baseZip, basePath, className);
                ZipEntry newEntry = getEntryFromClassNameOrThrow(newZip, newPath, className);
                try (InputStream baseIn = baseZip.getInputStream(baseEntry);
                     InputStream newIn = newZip.getInputStream(newEntry)) {
                    analyzer = DiffAnalyzer.analyze(baseIn, newIn);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            System.out.println("Class:" + className);
            analyzer.print(System.out);
        }

        private static Path toFilePath(String classpath) {
            switch (classpath) {
                case "oj":
                    return getAndroidBuildTop().resolve(
                            "out/soong/.intermediates/libcore/core-oj/android_common/"
                                    + "javac/core-oj.jar");
                case "8":
                    return getAndroidBuildTop().resolve(
                            "prebuilts/jdk/jdk8/linux-x86/jre/lib/rt.jar");
                case "9":
                case "11":
                case "17":
                    return getAndroidBuildTop().resolve(
                            "prebuilts/jdk/jdk" + classpath + "/linux-x86/jmods/java.base.jmod");
                default:
                    return Path.of(classpath);
            }
        }

        private static class DiffAnalyzer {
            List<MethodNode> newMethods = new ArrayList<>();

            List<MethodNode> removedMethods = new ArrayList<>();

            List<MethodNode> newlyDeprecatedMethods = new ArrayList<>();
            List<FieldNode> newFields = new ArrayList<>();

            List<FieldNode> removedFields = new ArrayList<>();

            List<FieldNode> newlyDeprecatedFields = new ArrayList<>();

            private static DiffAnalyzer analyze(InputStream baseIn, InputStream newIn)
                    throws IOException {
                ClassNode baseClass = parseClass(baseIn);
                ClassNode newClass = parseClass(newIn);
                Map<String, MethodNode> baseMethods = getExposedMethods(baseClass)
                        .collect(toMap(DiffAnalyzer::toApiSignature, node -> node));
                Map<String, MethodNode> newMethods = getExposedMethods(newClass)
                        .collect(toMap(DiffAnalyzer::toApiSignature, node -> node));

                DiffAnalyzer result = new DiffAnalyzer();
                result.newMethods = getExposedMethods(newClass)
                        .filter(node -> !baseMethods.containsKey(toApiSignature(node)))
                        .collect(toList());
                result.removedMethods = getExposedMethods(baseClass)
                        .filter(node -> !newMethods.containsKey(toApiSignature(node)))
                        .collect(toList());
                result.newlyDeprecatedMethods = getExposedMethods(newClass)
                        .filter(DiffAnalyzer::isDeprecated)
                        .filter(node -> !baseMethods.containsKey(toApiSignature(node))
                                || !isDeprecated(baseMethods.get(toApiSignature(node))) )
                        .collect(toList());


                Map<String, FieldNode> baseFields = getExposedFields(baseClass)
                        .collect(toMap(node -> node.name, node -> node));
                Map<String, FieldNode> newFields = getExposedFields(newClass)
                        .collect(toMap(node -> node.name, node -> node));

                result.newFields = getExposedFields(newClass)
                        .filter(node -> !baseFields.containsKey(node.name))
                        .collect(toList());
                result.removedFields = getExposedFields(baseClass)
                        .filter(node -> !newFields.containsKey(node.name))
                        .collect(toList());
                result.newlyDeprecatedFields = getExposedFields(newClass)
                        .filter(DiffAnalyzer::isDeprecated)
                        .filter(node -> !baseFields.containsKey(node.name)
                                || !isDeprecated(baseFields.get(node.name)) )
                        .collect(toList());

                return result;
            }

            /**
             * Known issue: this signature doesn't differentiate static and virtual methods.
             */
            private static String toApiSignature(MethodNode node) {
                return node.name + node.desc;
            }

            private static Stream<MethodNode> getExposedMethods(ClassNode classNode) {
                if (!isExposed(classNode.access)) {
                    return Stream.empty();
                }

                return classNode.methods.stream()
                        .filter(m -> isExposed(m.access));
            }

            private static Stream<FieldNode> getExposedFields(ClassNode classNode) {
                if (!isExposed(classNode.access)) {
                    return Stream.empty();
                }

                return classNode.fields.stream()
                        .filter(m -> isExposed(m.access));
            }

            private static boolean isExposed(int flags) {
                return (flags & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0;
            }

            private static boolean isDeprecated(MethodNode node) {
                return node.visibleAnnotations != null &&
                        node.visibleAnnotations.stream()
                                .anyMatch(anno ->"Ljava/lang/Deprecated;".equals(anno.desc));
            }

            private static boolean isDeprecated(FieldNode node) {
                return node.visibleAnnotations != null &&
                        node.visibleAnnotations.stream()
                                .anyMatch(anno ->"Ljava/lang/Deprecated;".equals(anno.desc));
            }

            private static ClassNode parseClass(InputStream in) throws IOException {
                ClassReader classReader = new ClassReader(in);
                ClassNode node = new ClassNode();
                classReader.accept(node,
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG| ClassReader.SKIP_FRAMES);
                return node;
            }

            void print(OutputStream out) {
                PrintWriter writer = new PrintWriter(out, /*autoFlush=*/true);

                printMethods(writer, "New methods", newMethods);
                printMethods(writer, "Removed methods", removedMethods);
                printMethods(writer, "Newly deprecated methods", newlyDeprecatedMethods);

                printFields(writer, "New fields", newFields);
                printFields(writer, "Removed fields", removedFields);
                printFields(writer, "Newly deprecated fields", newlyDeprecatedFields);
            }

            private static void printMethods(PrintWriter w, String header, List<MethodNode> nodes) {
                if (nodes.isEmpty()) {
                    return;
                }

                w.println(" " + header + ":");
                for (MethodNode node : nodes) {
                    w.println("  " + toApiSignature(node));
                }
            }

            private static void printFields(PrintWriter w, String header, List<FieldNode> nodes) {
                if (nodes.isEmpty()) {
                    return;
                }

                w.println(" " + header + ":");
                for (FieldNode node : nodes) {
                    w.println("  " + node.name + ": " + node.desc);
                }
            }
        }
    }

    public static void main(String[] argv) {
        MainArgs mainArgs = new MainArgs();
        CommandDump commandDump = new CommandDump();
        CommandApiDiff commandApiDiff = new CommandApiDiff();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(mainArgs)
                .addCommand(commandDump)
                .addCommand(commandApiDiff)
                .build();
        jCommander.parse(argv);

        if (mainArgs.help || jCommander.getParsedCommand() == null) {
            jCommander.usage();
            return;
        }

        switch (jCommander.getParsedCommand()) {
            case CommandDump.NAME:
                if (commandDump.help) {
                    jCommander.usage(CommandDump.NAME);
                } else {
                    commandDump.run();
                }
                break;
            case CommandApiDiff.NAME:
                if (commandApiDiff.help) {
                    jCommander.usage(CommandApiDiff.NAME);
                } else {
                    commandApiDiff.run();
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown sub-command: " +
                        jCommander.getParsedCommand());
        }
    }

    private static ZipEntry getEntryFromClassNameOrThrow(ZipFile zipFile, Path zipPath,
            String className) throws IllegalArgumentException {
        String entryName = className.replaceAll("\\.", "/") + ".class";
        ZipEntry e = zipFile.getEntry(entryName);
        if (e == null) {
            String secondName = "classes/" + entryName;
            e = zipFile.getEntry(secondName);
            if (e == null) {
                throw new IllegalArgumentException(String.format(Locale.US,
                        "Neither %s nor %s is found in %s", entryName, secondName,
                        zipPath.toString()));
            }
        }
        return e;
    }

    private static Path getAndroidBuildTop() {
        return pathFromEnvOrThrow("ANDROID_BUILD_TOP");
    }

    private static Path pathFromEnvOrThrow(String name) {
        String envValue = getEnvOrThrow(name);
        Path result = Paths.get(envValue);
        if (!result.toFile().exists()) {
            throw new IllegalStateException("Path not found: " + result);
        }
        return result;
    }

    private static String getEnvOrThrow(String name) {
        String result = System.getenv(name);
        if (result == null) {
            throw new IllegalStateException("Environment variable undefined: " + name);
        }
        return result;
    }
}
