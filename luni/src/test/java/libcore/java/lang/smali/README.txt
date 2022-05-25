This directory contains .smali files used to generate a .dex file used
by libcore.java.lang.ClassTest.

The use of .smali allows the generation of annotations for class metadata
that cannot be currently produced. For example the NestHost and NestMembers
annotations.

The files are included in core-tests-smali.dex through the
core-tests-smali-dex build component. Manually a .dex file can be generated
with:

make smali
smali assemble libcore/luni/src/test/java/libcore/java/lang/smali/*.smali \
    -o libcore/luni/src/test/resources/libcore/java/lang/smali/nestgroup.dex

---------------------

For reference, the classes started out as:

// NestGroupHost.java

package libcore.java.lang.nestgroup;

import dalvik.annotation.NestMembers;

@NestMembers(classes={NestGroupInnerA.class,
                      NestGroupB.class})
public class NestGroupHost {
}

// NestGroupInnerA.java

package libcore.java.lang.nestgroup;

import dalvik.annotation.NestHost;

@NestHost(host=NestGroupHost.class)
public class NestGroupInnerA {
}

// NestGroupB.java

package libcore.java.lang.nestgroup;

import dalvik.annotation.NestHost;

@NestHost(host=NestGroupB.class)
public class NestGroupB {
}

// NestGroupInnerFake.java

package libcore.java.lang.nestgroup;

import dalvik.annotation.NestHost;

@NestHost(host=NestGroupHost.class)
public class NestGroupInnerFake {
}

// NestGroupSelf.java

package libcore.java.lang.nestgroup;

import dalvik.annotation.NestHost;

public class NestGroupSelf {
}