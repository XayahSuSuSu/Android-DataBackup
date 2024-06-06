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

package libcore.util;

import androidx.annotation.NonNull;

import com.android.org.kxml2.io.KXmlParser;
import com.android.org.kxml2.io.KXmlSerializer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 * An internal class for creating platform-default XML parsers and related objects.
 *
 * @hide
 */
public class XmlObjectFactory {

    private XmlObjectFactory() {
    }

    /**
     * Returns a new instance of the platform default {@link XmlSerializer} more efficiently than
     * using {@code XmlPullParserFactory.newInstance().newSerializer()}.
     *
     * @return platform default {@link XmlSerializer}
     * @hide
     */
    public static @NonNull XmlSerializer newXmlSerializer() {
        return new KXmlSerializer();
    }

    /**
     * Returns a new instance of the platform default {@link XmlPullParser} more efficiently than
     * using {@code XmlPullParserFactory.newInstance().newPullParser()}.
     *
     * @return platform default {@link XmlPullParser}
     * @hide
     */
    public static @NonNull XmlPullParser newXmlPullParser() {
        return new KXmlParser();
    }
}