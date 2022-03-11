package net.fabricmc.loom.build;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.api.ModMetadataHelperAPI;
import net.fabricmc.loom.util.ZipUtils;

import org.slf4j.Logger;

public final class FabricModMetadataHelper implements ModMetadataHelperAPI {
	@Override
	public String getFileName() {
		return "fabric.mod.json";
	}

	@Override
	public Metadata createMetadata(File input) {
		return new Helper(readFabricModJson(input));
	}

	@Override
	public Metadata createMetadata(Path input) {
		return new Helper(readFabricModJson(input.toFile()));
	}

	@Override
	public Metadata createMetadata(Reader reader) {
		return new Helper(LoomGradlePlugin.GSON.fromJson(reader, JsonObject.class));
	}

	@Override
	public ZipUtils.UnsafeUnaryOperator<JsonObject> stripNestedJarsFunction() {
		return json -> {
			json.remove("jars");
			return json;
		};
	}

	@Override
	public ZipUtils.UnsafeUnaryOperator<JsonObject> addNestedJarsFunction(List<String> files) {
		return json -> {
			JsonArray nestedJars = json.getAsJsonArray("jars");

			if (nestedJars == null || !json.has("jars")) {
				nestedJars = new JsonArray();
			}

			for (String nestedJarPath : files) {
				for (JsonElement nestedJar : nestedJars) {
					JsonObject jsonObject = nestedJar.getAsJsonObject();

					if (jsonObject.has("file") && jsonObject.get("file").getAsString().equals(nestedJarPath)) {
						throw new IllegalStateException("Cannot nest 2 jars at the same path: " + nestedJarPath);
					}
				}

				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("file", nestedJarPath);
				nestedJars.add(jsonObject);

			}

			json.add("jars", nestedJars);

			return json;
		};
	}

	private JsonObject readFabricModJson(File input) {
		JsonObject jsonObject;
		try (var reader = new FileReader(input)) {
			jsonObject = LoomGradlePlugin.GSON.fromJson(reader, JsonObject.class);
		} catch (IOException e) {
			throw new UnsupportedOperationException("Failed to read fabric.mod.json file", e);
		}
		return jsonObject;
	}

	final class Helper implements Metadata {
		final JsonObject fabricModJson;
		public Helper(JsonObject fmj) {
			this.fabricModJson = fmj;
		}
		@Override
		public Collection<String> getMixinConfigurationFiles() {

			JsonArray mixins = fabricModJson.getAsJsonArray("mixins");

			if (mixins == null) {
				return Collections.emptyList();
			}

			return StreamSupport.stream(mixins.spliterator(), false)
					.map(e -> {
						if (e instanceof JsonPrimitive str) {
							return str.getAsString();
						} else if (e instanceof JsonObject obj) {
							return obj.get("config").getAsString();
						} else {
							throw new RuntimeException("Incorrect fabric.mod.json format");
						}
					}).collect(Collectors.toSet());
		}

		@Override
		public String getVersion() {
			if (!fabricModJson.has("version") || !fabricModJson.get("version").isJsonPrimitive()) {
				return null;
			}

			return fabricModJson.get("version").getAsString();
		}

		@Override
		public @Nullable String getName() {
			if (!fabricModJson.has("name") || !fabricModJson.get("name").isJsonPrimitive()) {
				return null;
			}
			return fabricModJson.get("name").getAsString();
		}

		@Override
		public @Nullable String getId() {
			if (!fabricModJson.has("id") || !fabricModJson.get("id").isJsonPrimitive()) {
				return null;
			}
			return fabricModJson.get("id").getAsString();

		}

		@Override
		public @Nullable String getAccessWidener() {
			if (!fabricModJson.has("accessWidener") || !fabricModJson.get("accessWidener").isJsonPrimitive()) {
				return null;
			}
			return fabricModJson.get("accessWidener").getAsString();
		}

		@Override
		public List<InjectedInterface> getInjectedInterfaces() {
			final String modId = getId();

			if (!fabricModJson.has("custom")) {
				return Collections.emptyList();
			}

			final JsonObject custom = fabricModJson.getAsJsonObject("custom");

			if (!custom.has("loom:injected_interfaces")) {
				return Collections.emptyList();
			}

			final JsonObject addedIfaces = custom.getAsJsonObject("loom:injected_interfaces");

			final List<InjectedInterface> result = new ArrayList<>();

			for (String className : addedIfaces.keySet()) {
				final JsonArray ifaceNames = addedIfaces.getAsJsonArray(className);

				for (JsonElement ifaceName : ifaceNames) {
					result.add(new InjectedInterface(modId, className, ifaceName.getAsString()));
				}
			}

			return result;
		}


		@Override
		public ModMetadataHelperAPI getParent() {
			return FabricModMetadataHelper.this;
		}


	}

}