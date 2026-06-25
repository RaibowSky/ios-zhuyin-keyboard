# Notices and Third-Party References

This file records external data sources and references used while developing the
project.

[繁體中文版本](NOTICE.zh-TW.md)

## Bundled Third-Party Data

### CC-CEDICT

The file `app/src/main/assets/zhuyin_cedict.tsv` is generated from CC-CEDICT.

- Source project: CC-CEDICT
- Download page: https://www.mdbg.net/chinese/dictionary?page=cc-cedict
- Project/editor page: https://cc-cedict.org/
- License: Creative Commons Attribution-ShareAlike 4.0 International
  (CC BY-SA 4.0)
- License URL: https://creativecommons.org/licenses/by-sa/4.0/
- Local conversion script: `tools/build_zhuyin_dictionary.py`

Transformation performed:

- Read Traditional Chinese entries and Pinyin readings from `cedict.txt.gz`.
- Convert Pinyin syllables into Zhuyin symbols and tone marks.
- Generate keyed candidate rows in `key<TAB>candidate1 candidate2 ...` format.
- Include both toned and untoned lookup keys for IME candidate lookup.

The generated file is a derived data asset and should continue to carry
CC-CEDICT attribution and compatible license handling when redistributed.

## Non-Bundled References

This project was implemented independently.

During development, publicly available Chinese input methods and linguistic
resources were consulted to understand general input workflows, keyboard
interaction patterns, and Zhuyin conventions.

These references were used only for behavior study, design comparison, and
linguistic validation. Unless explicitly documented in the bundled third-party
data section above, this repository does not include third-party source code,
proprietary dictionaries, visual assets, trademark assets, scraped datasets, or
other copyrighted materials from those references.

The currently bundled candidate dictionary is generated only from CC-CEDICT as
documented above.

## Repository Policy for Future Data

Before adding any new dictionary, word-frequency list, keyboard layout asset, or
other third-party material:

1. Confirm the license permits the intended use.
2. Add source URL, retrieval date, and license terms to this file.
3. Keep generated data separate from scripts when possible.
4. Document transformations clearly enough that the data can be regenerated.
5. Do not commit scraped/proprietary data unless the license is understood and
   compatible with redistribution.
