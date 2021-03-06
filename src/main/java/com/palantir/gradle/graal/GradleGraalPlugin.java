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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.provider.Provider;

/**
 * Adds tasks to download, extract and interact with GraalVM tooling.
 *
 * <p>All tooling execution (e.g. nativeImage) will cause GraalVM tooling to download and cache if not already
 * present. Currently, GraalVM CE only supports MacOS and Linux, and, as a result, this plugin will only correctly
 * function on MacOS and Linux. The plugin will automatically select the correct architecture and error clearly
 * if the runtime architecture is not supported.</p>
 *
 * <p>Downloads are stored in ~/.gradle/caches/com.palantir.graal using the following structure:</p>
 * <pre>
 * ~/.gradle/caches/com.palantir.graal/
 * └── [version]/
 *     ├── graalvm-ce-[version]/
 *     │   └── [local architecture-specific GraalVM tooling]
 *     └── graalvm-ce-[version]-amd64.tar.gz
 * </pre>
 */
public class GradleGraalPlugin implements Plugin<Project> {

    /** Location to cache downloaded and extracted GraalVM tooling. */
    public static final Path CACHE_DIR =
            Paths.get(System.getProperty("user.home"), ".gradle", "caches", "com.palantir.graal");

    public static final String TASK_GROUP = "Graal";

    @Override
    public final void apply(Project project) {
        GraalExtension extension = project.getExtensions().create("graal", GraalExtension.class, project);

        DownloadGraalTask downloadGraal = project.getTasks().create(
                "downloadGraalTooling", DownloadGraalTask.class, task -> {
                    task.configure(extension.getGraalVersion(), extension.getDownloadBaseUrl());
                });

        ExtractGraalTask extractGraal = project.getTasks().create(
                "extractGraalTooling", ExtractGraalTask.class, task -> {
                    task.dependsOn(downloadGraal);
                    task.configure(asProvider(() -> downloadGraal.getOutput().toFile()), extension.getGraalVersion());
                });

        project.getTasks().create("nativeImage", NativeImageTask.class, task -> {
            task.dependsOn(extractGraal);
            task.dependsOn("jar");
            task.configure(extension.getMainClass(), extension.getOutputName(), extension.getGraalVersion());
        });
    }

    private static <T> Provider<T> asProvider(Supplier<T> supplier) {
        return new Provider<T>() {
            @Override
            public T get() {
                return supplier.get();
            }

            @Nullable
            @Override
            public T getOrNull() {
                return supplier.get();
            }

            @Override
            public T getOrElse(T other) {
                return supplier.get();
            }

            @Override
            public <S> Provider<S> map(Transformer<? extends S, ? super T> transformer) {
                return asProvider(() -> transformer.transform(get()));
            }

            @Override
            public boolean isPresent() {
                return true;
            }
        };
    }
}
