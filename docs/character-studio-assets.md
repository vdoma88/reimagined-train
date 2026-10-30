# Illustrated character layers

Four transparent PNG layers added for the character editor, generated with the built-in image generation tool using the existing game sprites as geometry and visual-style references. The original assets remain unchanged.

- `app/src/main/assets/cartoon/43_hair_indigo_mint.png`: front-facing tousled indigo hair with a mint streak, transparent face opening and ears aligned to `05_hair_short_chestnut.png`.
- `app/src/main/assets/cartoon/44_hair_honey_waves.png`: front-facing honey-blond waves with teal barrette, transparent face opening and ears aligned to `05_hair_short_chestnut.png`.
- `app/src/main/assets/cartoon/45_top_plum_stars.png`: plum bomber jacket with gold piping and star, cream shirt, geometry referenced to `09_top_teal_jacket.png`.
- `app/src/main/assets/cartoon/46_top_sage_cardigan.png`: sage knitted cardigan with leaf patch and cream shirt, geometry referenced to `09_top_teal_jacket.png`.

Generation prompt pattern: isolated transparent modular hair or clothing sprite for a character creator; match the supplied reference's front-facing alignment, face/neck opening, proportions, sleeves, ears and position; warm hand-painted mystery-adventure cartoon style, crisp brown contours and soft shading; no face/body in the hair asset, no head/hands/body in the garment, no text or background. Color and garment details are specified per item above.

Placement uses `cartoon_layers.json` in the shared 400×640 rig. Clothing overlap is checked at the waistband for every top/bottom pairing. Source PNGs remain full-resolution; the renderer decodes these four at half resolution to reduce memory use.
