# Illustrated characters

Three offline full-height cutouts derived from the user-supplied character references: classic anime, modern manga, and adventure cartoon. Generated with the built-in imagegen tool; packed as 1024×1536 WebP assets with the original alpha in `app/src/main/res/drawable-nodpi/character_*.webp`. No network dependency.

Prompt set (one call per supplied style/identity): Preserve the reference person, face, hair, age, clothing and art style. Draw one complete full-height standing character, frontal three-quarter pose, friendly neutral expression, detailed hair strands, face shading, garment folds and accessories. Center on a genuinely transparent portrait canvas, keep the whole silhouette, no duplicate portrait, panels, text or floor. No chibi simplification.

`Appearance.illustrationId` is optional. Missing/unknown IDs keep the existing procedural renderer; legacy characters are not silently replaced. New creator sessions start with classic illustrated art. The chat menu lets users select artwork or restore their original custom appearance. Atomic DAO writes update only appearance JSON, preserving concurrently updated memory and message metadata.

Illustrated art has a fixed face and costume, with gentle breathing movement; it does not implement lip sync or facial emotion replacement. The existing procedural constructor retains those capabilities. LLM appearance descriptions follow the displayed illustration rather than the hidden customizable parameters.

Portrait, full-height and thumbnail crops are drawn from the same packaged image. Only the compositing bounds change; there are no duplicate decoded portrait resources. Preview tests cover all three artwork IDs, full-height silhouettes, bust crops, and 44/72dp avatars on a light background.
