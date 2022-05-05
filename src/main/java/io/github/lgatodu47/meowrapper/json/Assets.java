package io.github.lgatodu47.meowrapper.json;

import java.util.Map;

/**
 * Java representation of an asset index Json file.
 */
public class Assets {
    /**
     * A map with the name of the asset as key and with an AssetInfo as value.
     */
    public Map<String, AssetInfo> objects;

    /**
     * Java representation of an asset info.
     */
    public static class AssetInfo {
        /**
         * The hash of the asset described by this info.
         */
        public String hash;
    }
}
