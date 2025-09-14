package io.github.ngloc01.pack_battle_app.dataTransferObject;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScryfallCard {
    private String name;

    @JsonProperty("image_uris")
    private ImageUris imageUris;

    @JsonProperty("prices")  
    private Price prices;

    public String getName() {
        return name;
    }

    public ImageUris getImageUris() {
        return imageUris;
    }

    public Price getPrices() {
        return prices;
    }

    public static class ImageUris {
        private String normal;

        public String getNormal() {
            return normal;
        }
    }

    public static class Price {
        private String usd; 

        public String getUsd() {
            return usd;
        }
    }
}

