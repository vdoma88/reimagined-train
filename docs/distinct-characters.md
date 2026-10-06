# Разные персонажи и проверка конструктора

Варианты: мужчина и девушка. Три отдельных лица для каждого пола, два комплекта глаз, три телосложения. Мужские образы отличаются челюстью, носом, бровями, шириной плеч и соотношением головы и тела. Свободный ручной выбор причёсок и одежды остаётся.

Исправления: жилет закрывает стык с поясом юбки; кепка и шапка подняты над глазами; вся одежда, руки, ноги и обувь используют общий масштаб тела; лицо, глаза, рот, причёска и аксессуары используют общий масштаб головы. Моргание использует те же координаты.

Проверка: все 42 зарегистрированных слоя помещаются в кадр 400×640 для обоих полов и трёх телосложений. Просмотрены 16 собранных образов, покрывающие все предметы одежды и аксессуаров. Репродукция: `node tools/preview_cartoon.cjs`; код рендера повторяет координаты и масштаб Android. Android-тесты и Paparazzi выполняются в CI.

Старый NEUTRAL остаётся только как совместимое значение сохранённых данных; в приложении оно открывается как девушка. Выбор в конструкторе содержит только два варианта. История и данные персонажа сохраняются.

## Новая графика

Создана встроенным imagegen с настоящей прозрачностью. Атласы разделены на PNG, подготовлены по размеру и сжаты для приложения. Исходные изображения сохранены отдельно.

- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/33_face_female_0.png
- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/34_face_female_1.png
- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/35_face_female_2.png
- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/36_face_male_0.png
- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/37_face_male_1.png
- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/38_face_male_2.png
- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/39_eyes_female_0.png
- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/40_eyes_female_1.png
- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/41_eyes_male_0.png
- /workspace/scratch/a2fcf538ab7a/integration/app/src/main/assets/cartoon/42_eyes_male_1.png

## Промпты

### Лица

Use case: stylized-concept. Transparent modular face sprite atlas for an original forest mystery cartoon character editor. Reference: 02_face_blank.png, dark brown outline, warm peach skin, frontal view, small ears, no neck. One atlas, 3 columns × 2 rows, generous clear transparent gaps. Top row: feminine heart-shaped narrow jaw and tiny upturned nose; round cheeks and button nose; long oval refined chin and slender nose. Bottom row: masculine broad square jaw and wider nose; lean angular chin and long straight nose; sturdy broad round jaw and bulbous nose. Structurally distinct contours, not scaled copies. Same peach skin on all six. Bald heads, no hair, eyes, eyebrows, eyelashes, mouth, clothing or labels. Nose centered near 70 percent of head height, mouth slot below. Actual transparent alpha.

### Глаза

Use case: stylized-concept. Transparent 2×2 modular eye sprite atlas for the same original forest cartoon kit. Reference: 03_eyes_calm.png. Only isolated eye pairs with eyebrows, generous transparent padding. Dark brown outline, off-white sclera, near-black pupils, frontal aligned gaze. Top left: feminine rounded open eyes, two outward lashes and slim arched brows. Top right: feminine almond eyes, outer lashes and slim expressive brows. Bottom left: masculine slightly squared large oval eyes, no lashes, thick straight brows. Bottom right: masculine narrower eyes, broad brows, calm amused gaze, no lashes. Same spacing and alignment as the reference rig. No head, nose, mouth, skin patches, accessories or text. True transparent alpha.
