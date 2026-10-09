package com.srirammart.util;

import java.util.Map;
import org.springframework.stereotype.Component;

/** Image helpers for templates: ${@assets.tile(slug)} and ${@assets.banner(slug)}. */
@Component("assets")
public class Assets {
    private static final Map<String, String> TILES = Map.of(
            "mobiles-tablets", "samsung-galaxy-m35-5g",
            "computers-accessories", "hp-pavilion-15",
            "fashion", "nike-mens-air-force-1-sneakers",
            "home-living", "philips-air-fryer",
            "beauty-personal-care", "loreal-paris-revitalift-serum",
            "sports-fitness", "nike-revolution-7-running-shoes",
            "books", "atomic-habits",
            "toys-games", "soft-teddy-bear-30-cm",
            "groceries", "fresh-fruits-and-vegetables-box");

    public String tile(String slug) { return "/img/products/" + TILES.getOrDefault(slug, "atomic-habits") + ".jpg"; }
    public String banner(String slug) { return "/img/banners/cat-" + slug + ".jpg"; }
}
