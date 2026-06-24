"""
Standalone verification of the LinkParser / MusicPlatform.detect logic.
Mirrors the Kotlin implementation so we can sanity-check detection without an
Android toolchain. This is a dev-only script, not shipped in the app.
"""
import re

HOST_PATTERNS = {
    "SPOTIFY": ["open.spotify.com", "spotify.link"],
    "YOUTUBE_MUSIC": ["music.youtube.com"],
    "APPLE_MUSIC": ["music.apple.com"],
    "DEEZER": ["deezer.com", "deezer.page.link", "dzr.page.link"],
    "TIDAL": ["tidal.com", "listen.tidal.com"],
    "AMAZON_MUSIC": ["music.amazon.com", "amazon.com/music"],
}
YOUTUBE_GENERIC = ["youtube.com/watch", "youtu.be"]

URL_RE = re.compile(r"https?://[^\s]+", re.IGNORECASE)


def detect(url: str):
    n = url.lower()
    for platform, patterns in HOST_PATTERNS.items():
        if any(p in n for p in patterns):
            return platform
    if any(p in n for p in YOUTUBE_GENERIC):
        return "YOUTUBE_MUSIC"
    return None


def parse(text):
    if not text or not text.strip():
        return None
    for m in URL_RE.finditer(text):
        candidate = m.group(0).rstrip(".,)]\"'>")
        p = detect(candidate)
        if p:
            return (candidate, p)
    return None


CASES = [
    # (input, expected_platform_or_None)
    ("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT", "SPOTIFY"),
    ("Check this out https://open.spotify.com/track/123?si=abc 🎵", "SPOTIFY"),
    ("https://music.apple.com/us/album/song/123?i=456", "APPLE_MUSIC"),
    ("https://music.youtube.com/watch?v=abc", "YOUTUBE_MUSIC"),
    ("https://youtu.be/dQw4w9WgXcQ", "YOUTUBE_MUSIC"),
    ("https://www.youtube.com/watch?v=dQw4w9WgXcQ", "YOUTUBE_MUSIC"),
    ("https://www.deezer.com/track/3135556", "DEEZER"),
    ("https://deezer.page.link/abcd", "DEEZER"),
    ("https://tidal.com/browse/track/12345", "TIDAL"),
    ("https://listen.tidal.com/track/12345", "TIDAL"),
    ("https://music.amazon.com/albums/B01ABC?trackAsin=B02XYZ", "AMAZON_MUSIC"),
    ("Look: https://open.spotify.com/track/xyz.", "SPOTIFY"),  # trailing period
    ("(https://tidal.com/browse/track/1)", "TIDAL"),           # wrapped in parens
    # Negatives -> None (treated as empty state, never an error)
    ("https://example.com/article", None),
    ("just some text without a link", None),
    ("https://github.com/some/repo", None),
    ("", None),
    (None, None),
    ("https://www.google.com/search?q=spotify", None),  # mentions spotify but not a music host
]


def main():
    failures = 0
    for text, expected in CASES:
        result = parse(text)
        got = result[1] if result else None
        ok = got == expected
        if not ok:
            failures += 1
        status = "PASS" if ok else "FAIL"
        shown = (text[:55] + "…") if text and len(text) > 56 else text
        print(f"[{status}] expected={expected!s:<14} got={got!s:<14} input={shown!r}")
    print("-" * 60)
    if failures:
        print(f"{failures} FAILED out of {len(CASES)}")
        raise SystemExit(1)
    print(f"All {len(CASES)} cases passed.")


if __name__ == "__main__":
    main()
