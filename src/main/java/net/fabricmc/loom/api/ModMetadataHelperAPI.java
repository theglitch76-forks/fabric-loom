package net.fabricmc.loom.api;

import com.google.gson.JsonObject;

import net.fabricmc.loom.util.ZipUtils;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

@ApiStatus.Experimental // This API may change at any time as new features are added to Loom
public interface ModMetadataHelperAPI extends Serializable {
	String getFileName();

	/**
	 * @throws UnsupportedOperationException if the mod metadata cannot be read
	 */
	Metadata createMetadata(File input);
	Metadata createMetadata(Path input);
	Metadata createMetadata(Reader reader);

	ZipUtils.UnsafeUnaryOperator<JsonObject> stripNestedJarsFunction();

	/**
	 * @param files
	 * @return
	 * @throws IllegalStateException if any duplicated nested jars are added
	 */
	ZipUtils.UnsafeUnaryOperator<JsonObject> addNestedJarsFunction(List<String> files);

	interface Metadata {
		ModMetadataHelperAPI getParent();
		Collection<String> getMixinConfigurationFiles();

		/**
		 * @return null if the provided mod metadata does not include a version
		 */
		@Nullable
		String getVersion();

		/**
		 * The name of this mod, e.g. "Fabric Example Mod"
		 * @return null if the provided mod metadata does not include a name
		 */
		@Nullable
		String getName();

		@Nullable
		String getId();

		@Nullable
		String getAccessWidener();
		List<InjectedInterface> getInjectedInterfaces();

		record InjectedInterface(String modId, String className, String ifaceName) {}
	}

}
