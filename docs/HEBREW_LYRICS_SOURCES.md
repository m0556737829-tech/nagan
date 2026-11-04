# Hebrew lyrics catalog: sources and import policy

Last reviewed: 2026-11-04

## Included now

- **התקווה — נפתלי הרץ אימבר**: first stanza only.
- Project Ben-Yehuda's collection page marks the Imber collection as public domain:
  https://benyehuda.org/collections/6839
- The source page identifies the first stanza under the entry for "תקוותנו (התקווה)". The catalog stores plain text, not synchronized timestamps.
- The bundled entry is keyed by both the known author and a generic artist fallback because Android music tags vary.

## Candidate dataset not imported

- **Hebrew Songs Lyrics (Kaggle, Guy Barash)**: approximately 15,000 Hebrew songs, described in the NNLP-IL Hebrew resources directory as scraped from Shironet and labeled CC BY-SA 4.0.
- Dataset page: https://www.kaggle.com/datasets/guybarash/hebrew-songs-lyrics
- Directory entry: https://github.com/NNLP-IL/Hebrew-Resources/blob/master/corpora_and_data_resources.rst

The dataset's license label is not sufficient evidence that the dataset publisher had permission to relicense the underlying lyrics. Shironet is a source of copyrighted song lyrics. Do not copy this corpus into the APK unless the rights holder or dataset publisher provides documentation that covers redistribution of the underlying lyrics in an offline app.

## Import policy

1. Prefer public-domain lyrics with a source that explicitly identifies their public-domain status, or lyrics released under a license that clearly permits redistribution in an offline Android app.
2. Keep attribution and license metadata alongside every imported work.
3. Do not assume that a website being publicly readable, or a dataset being labeled CC BY-SA, automatically grants rights to redistribute the underlying lyrics.
4. Synchronized LRC timestamps should be added only from an authorized source or created by the user.
5. The app must continue to work fully offline; no runtime lyrics requests are made.

## Current limitation

The bundled Hebrew catalog is intentionally small. It is better to ship a small verified catalog than to silently include thousands of lyrics with unclear rights. The in-app manual editor remains available for lyrics the user is authorized to use.
