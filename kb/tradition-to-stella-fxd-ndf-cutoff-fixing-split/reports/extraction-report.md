# Extraction Report

This draft pack was reconstructed from 16 photographed screenshots. macOS Vision OCR was used for first-pass extraction and then corrected against visible YAML fragments.

## Reconstructed Objects

- Lookup: `lk_fxd_ndf_cutoff_by_pair_and_locode`
- Helper: `hlp_fxd_ndf_fixing_quoted_currency_pair`
- Rules: fixing block, quoted currency pair, fixing date, primary rate source, secondary rate source, fixing time

## Reconstruction Notes

- Long target XPaths were visibly truncated in several photos. They were regenerated under the shared target root shown in README/metadata.
- The lookup filename appears as `Ik_...` in OCR, but the object is normalized to `lk_...` because this is the likely lowercase lookup prefix.
- The workbook-visible misspelling `PRIMARY_SOUCE_*` is retained in lookup return-field names because it appears to be the source column spelling.
- Photo OCR sometimes confused `xsl`/`xslt`, `StaticMap`/`StaticMapping`, and uppercase/lowercase Java members. Evidence entries were normalized to the most likely code identifiers.
