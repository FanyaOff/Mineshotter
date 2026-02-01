package com.fanya.mineshotter.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MineshotterModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            MineshotterConfig config = MineshotterConfig.get();
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.of("Mineshotter Configuration"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));

            general.addEntry(entryBuilder.startEnumSelector(
                    Text.of("Uploader Service"),
                    MineshotterConfig.UploaderMode.class,
                    config.mode
            )
                    .setDefaultValue(MineshotterConfig.UploaderMode.KAPPA_LOL)
                    .setTooltip(Text.of("Choose builtin uploader or a custom one"))
                    .setSaveConsumer(newValue -> config.mode = newValue)
                    .build());

            ConfigCategory customCategory = builder.getOrCreateCategory(Text.of("Custom Uploader"));

            customCategory.addEntry(entryBuilder.startStrField(
                    Text.of("Request URL"),
                    config.customProfile.requestUrl
            )
                    .setDefaultValue("")
                    .setTooltip(Text.of("The POST URL to upload to"))
                    .setSaveConsumer(val -> config.customProfile.requestUrl = val)
                    .build());

            customCategory.addEntry(entryBuilder.startStrField(
                    Text.of("File Form Name"),
                    config.customProfile.fileFormName
            )
                    .setDefaultValue("file")
                    .setTooltip(Text.of("The form-data name for the file part"))
                    .setSaveConsumer(val -> config.customProfile.fileFormName = val)
                    .build());

            customCategory.addEntry(entryBuilder.startStrField(
                    Text.of("JSON Result Path"),
                    config.customProfile.urlResultPath
            )
                    .setDefaultValue("{json:url}")
                    .setTooltip(Text.of("Pattern to find URL in response. e.g. {json:data.link}"))
                    .setSaveConsumer(val -> config.customProfile.urlResultPath = val)
                    .build());

            String argsStr = config.customProfile.arguments.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(";"));

            customCategory.addEntry(entryBuilder.startStrField(
                            Text.of("Arguments"),
                            argsStr
                    )
                    .setDefaultValue("")
                    .setTooltip(Text.of("Extra POST arguments in format: key=value;key2=value2"))
                    .setSaveConsumer(val -> {
                        Map<String, String> map = new HashMap<>();
                        if (val != null && !val.isEmpty()) {
                            String[] pairs = val.split(";");
                            for (String pair : pairs) {
                                String[] parts = pair.split("=", 2);
                                if (parts.length == 2) {
                                    map.put(parts[0].trim(), parts[1].trim());
                                }
                            }
                        }
                        config.customProfile.arguments = map;
                    })
                    .build());

            builder.setSavingRunnable(MineshotterConfig::save);

            return builder.build();
        };
    }
}
