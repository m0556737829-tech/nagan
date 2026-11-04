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

## Additional Hebrew sources added to the project

### Project Ben-Yehuda public-domain dump

Repository: https://github.com/projectbenyehuda/public_domain_dump

The repository describes a dump of more than 20,000 Hebrew public-domain works, supplied as UTF-8 text files with a catalogue of titles, authors, genres and paths. Its README and LICENSE state that the included data files are public domain and may be reused freely. This is now registered in `app/src/main/assets/lyrics_sources.json`.

This is a much stronger source for expanding the Hebrew collection than copying from lyric websites. The catalogue includes literary works, not only songs, so entries must be filtered for actual song/poem suitability before import.

### Wikisource public-domain song index

Index: https://he.wikisource.org/wiki/ויקיטקסט:שירים_ללא_זכויות_יוצרים

The page lists song titles and identifies their lyrics and melodies as public domain. It is useful for discovering additional Hebrew works, but it is an index rather than a complete downloadable lyric corpus. Each full text still needs to be located and verified before it is copied into NAGAN.

### Important distinction

Project Ben-Yehuda's public-domain dump is separate from the full Project Ben-Yehuda website database. The project's own policy says some website entries are only displayed there under special permission and cannot be redistributed. Only the public-domain dump / individually marked public-domain works should be imported.

The source registry is stored in `app/src/main/assets/lyrics_sources.json`. The current lyrics catalog remains intentionally limited to verified texts; adding a source to the registry does not mean its entire content has been imported.

## Additional song imported

- **שיר העבודה והמלאכה — חיים נחמן ביאליק**: added as plain-text lyrics from https://benyehuda.org/read/7553. The work page marks it "נחלת הכלל" and tags it as a composed song. The entry is keyed by title and artist, with a title-only fallback.
