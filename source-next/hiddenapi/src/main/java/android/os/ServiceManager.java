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

package android.os;

/**
 * Manage binder services as registered with the binder context manager. These services must be
 * declared statically on an Android device (SELinux access_vector service_manager, w/ service
 * names in service_contexts files), and they do not follow the activity lifecycle. When
 * building applications, android.app.Service should be preferred.
 *
 * @hide
 **/
public final class ServiceManager {
    /**
     * Returns a reference to a service with the given name.
     *
     * @param name the name of the service to get
     * @return a reference to the service, or <code>null</code> if the service doesn't exist
     * @hide
     */
    public static IBinder getService(String name) {
        throw new RuntimeException("Stub!");
    }
}
