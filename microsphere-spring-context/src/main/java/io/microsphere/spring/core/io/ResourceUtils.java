/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.microsphere.spring.core.io;

import io.microsphere.util.Utils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;

/**
 * The utilities class for {@link Resource}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resource
 * @since 1.0.0
 */
public abstract class ResourceUtils implements Utils {

    /**
     * Determine whether the specified {@link Resource} is a {@linkplain org.springframework.core.io.FileUrlResource}
     *
     * @param resource {@link Resource}
     * @return <code>true</code> if the specified {@link Resource} is a {@linkplain org.springframework.core.io.FileUrlResource}
     */
    public static boolean isFileUrlResource(Resource resource) {
        return resource instanceof FileUrlResource;
    }

    /**
     * Determine whether the specified {@link Resource} is a file based {@link Resource}
     *
     * @param resource {@link Resource}
     * @return <code>true</code> if the specified {@link Resource} is a file system based {@link Resource}
     */
    public static boolean isFileBasedResource(Resource resource) {
        return resource instanceof FileSystemResource ||
                isFileUrlResource(resource);
    }

    private ResourceUtils() {
    }
}
