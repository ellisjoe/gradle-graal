/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.graal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.gradle.api.DefaultTask;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Downloads GraalVM binaries to {@link GradleGraalPlugin#CACHE_DIR}. */
public class DownloadGraalTask extends DefaultTask {

    private static final String ARTIFACT_PATTERN = "[url]/vm-[version]/graalvm-ce-[version]-[os]-[arch].tar.gz";
    private static final String FILENAME_PATTERN = "graalvm-ce-[version]-[arch].tar.gz";

    @Input private Provider<String> graalVersion;
    @Input private Provider<String> downloadBaseUrl;

    @TaskAction
    public final void downloadGraal() throws IOException {
        Path cache = getCache();
        if (!(cache.toFile().mkdirs() || cache.toFile().exists())) {
            throw new IllegalStateException(
                    "Unable to make cache directory, and the cache directory does not already exist: " + cache);
        }

        InputStream in = new URL(render(ARTIFACT_PATTERN)).openStream();
        Files.copy(in, getOutput(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public final String getGroup() {
        return GradleGraalPlugin.TASK_GROUP;
    }

    @Override
    public final String getDescription() {
        return "Downloads and caches GraalVM binaries.";
    }

    @OutputFile
    public final Path getOutput() {
        return getCache().resolve(render(FILENAME_PATTERN));
    }

    /** Returns a lambda that prevents this task from running if the download target already exists in the cache. */
    @Override
    public final Spec<? super TaskInternal> getOnlyIf() {
        return spec -> !getOutput().toFile().exists();
    }

    @SuppressWarnings("checkstyle:hiddenfield")
    public final void configure(Provider<String> graalVersion, Provider<String> downloadBaseUrl) {
        this.graalVersion = graalVersion;
        this.downloadBaseUrl = downloadBaseUrl;
    }

    private Path getCache() {
        return GradleGraalPlugin.CACHE_DIR.resolve(graalVersion.get());
    }

    private String render(String pattern) {
        return pattern
                .replaceAll("\\[url\\]", downloadBaseUrl.get())
                .replaceAll("\\[version\\]", graalVersion.get())
                .replaceAll("\\[os\\]", getOperatingSystem())
                .replaceAll("\\[arch\\]", getArchitecture());
    }

    private String getOperatingSystem() {
        switch (Platform.operatingSystem()) {
            case MAC:
                return "macos";
            case LINUX:
                return "linux";
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        }
    }

    private String getArchitecture() {
        switch (Platform.architecture()) {
            case AMD64:
                return "amd64";
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.architecture());
        }
    }
}
