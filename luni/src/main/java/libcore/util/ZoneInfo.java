/*
 * Copyright (C) 2007 The Android Open Source Project
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
/*
 * Elements of the WallTime class are a port of Bionic's localtime.c to Java. That code had the
 * following header:
 *
 * This file is in the public domain, so clarified as of
 * 1996-06-05 by Arthur David Olson.
 */
package libcore.util;

import android.compat.annotation.UnsupportedAppUsage;

import com.android.i18n.timezone.ZoneInfoData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.TimeZone;

/**
 *  Our concrete TimeZone implementation, backed by a {@link ZoneInfoData}. This class is not
 *  thread-safe.
 *
 * This class exists in this package and has certain fields / a defined serialization footprint for
 * app compatibility reasons. The knowledge of the underlying file format has been split out into
 * {@link ZoneInfoData} which is intended to be updated independently of the classes in
 * libcore.util.
 *
 * @hide - used to implement TimeZone
 */
public final class ZoneInfo extends TimeZone {
    private static final long MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;
    private static final long MILLISECONDS_PER_400_YEARS =
            MILLISECONDS_PER_DAY * (400 * 365 + 100 - 3);

    private static final long UNIX_OFFSET = 62167219200000L;

    private static final int[] NORMAL = new int[] {
            0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334,
    };

    private static final int[] LEAP = new int[] {
            0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335,
    };

    // Proclaim serialization compatibility with pre-OpenJDK AOSP
    static final long serialVersionUID = -4598738130123921552L;

    /**
     * Keep the serialization compatibility even though the fields have been moved to
     * {@link ZoneInfoData}.
     */
    private static final ObjectStreamField[] serialPersistentFields =
        ZoneInfoData.ZONEINFO_SERIALIZED_FIELDS;

    /**
     * This field is kept only for app compatibility indicated by @UnsupportedAppUsage. Do not
     * modify the content of this array as it is a reference to an internal data structure used by
     * mDelegate.
     */
    @UnsupportedAppUsage
    private final long[] mTransitions;

    /**
     * Despite being transient, mDelegate is still serialized as part of this object. Please
     * see {@link #readObject(ObjectInputStream)} and {@link #writeObject(ObjectOutputStream)}
     */
    private transient ZoneInfoData mDelegate;

    public ZoneInfo(ZoneInfoData delegate) {
        mDelegate = delegate;
        mTransitions = delegate.getTransitions();
        setID(delegate.getID());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField getField = in.readFields();
        // TimeZone#getID() should return the proper ID because the fields in the superclass should
        // have been deserialized.
        mDelegate = ZoneInfoData.createFromSerializationFields(getID(), getField);

        // Set the final field mTransitions by reflection.
        try {
            Field mTransitionsField = ZoneInfo.class.getDeclaredField("mTransitions");
            mTransitionsField.setAccessible(true);
            mTransitionsField.set(this, mDelegate.getTransitions());
        } catch (ReflectiveOperationException e) {
            // mTransitions should always exists because it's a member field in this class.
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        PutField putField = out.putFields();
        mDelegate.writeToSerializationFields(putField);
        out.writeFields();
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis) {
        // XXX This assumes Gregorian always; Calendar switches from
        // Julian to Gregorian in 1582.  What calendar system are the
        // arguments supposed to come from?

        long calc = (year / 400) * MILLISECONDS_PER_400_YEARS;
        year %= 400;

        calc += year * (365 * MILLISECONDS_PER_DAY);
        calc += ((year + 3) / 4) * MILLISECONDS_PER_DAY;

        if (year > 0) {
            calc -= ((year - 1) / 100) * MILLISECONDS_PER_DAY;
        }

        boolean isLeap = (year == 0 || (year % 4 == 0 && year % 100 != 0));
        int[] mlen = isLeap ? LEAP : NORMAL;

        calc += mlen[month] * MILLISECONDS_PER_DAY;
        calc += (day - 1) * MILLISECONDS_PER_DAY;
        calc += millis;

        calc -= mDelegate.getRawOffset();
        calc -= UNIX_OFFSET;

        return mDelegate.getOffset(calc);
    }

    @Override
    public int getOffset(long when) {
        return mDelegate.getOffset(when);
    }

    @Override
    public boolean inDaylightTime(Date time) {
        return mDelegate.isInDaylightTime(time.getTime());
    }

    @Override
    public int getRawOffset() {
        return mDelegate.getRawOffset();
    }

    @Override
    public void setRawOffset(int off) {
        mDelegate = mDelegate.createCopyWithRawOffset(off);
    }

    @Override
    public int getDSTSavings() {
        return mDelegate.getDSTSavings();
    }

    @Override
    public boolean useDaylightTime() {
        return mDelegate.useDaylightTime();
    }

    @Override
    public boolean hasSameRules(TimeZone timeZone) {
        if (!(timeZone instanceof ZoneInfo)) {
            return false;
        }
        ZoneInfo other = (ZoneInfo) timeZone;
        return mDelegate.hasSameRules(other.mDelegate);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ZoneInfo)) {
            return false;
        }
        ZoneInfo other = (ZoneInfo) obj;
        return getID().equals(other.getID()) && hasSameRules(other);
    }

    @Override
    public int hashCode() {
        /*
         * TODO Is it an existing bug? Can 2 ZoneInfo objects have different hashCode but equals?
         * mDelegate.hashCode compares more fields than rules and ID.
         */
        return mDelegate.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + mDelegate.toString();
    }

    @Override
    public Object clone() {
        return new ZoneInfo(mDelegate.createCopy());
    }

    public int getOffsetsByUtcTime(long utcTimeInMillis, int[] offsets) {
        return mDelegate.getOffsetsByUtcTime(utcTimeInMillis, offsets);
    }
}
