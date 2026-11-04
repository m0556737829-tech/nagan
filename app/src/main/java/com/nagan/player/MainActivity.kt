package com.nagan.player

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.util.Locale

private data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri
)

private data class LyricLine(val timeMs: Long, val text: String)

class MainActivity : Activity() {
    private val bg = Color.rgb(9, 11, 18)
    private val panel = Color.rgb(22, 26, 39)
    private val accent = Color.rgb(115, 92, 255)
    private val muted = Color.rgb(153, 160, 180)
    private val white = Color.rgb(246, 247, 255)

    private lateinit var songList: LinearLayout
    private lateinit var lyricsView: TextView
    private lateinit var titleView: TextView
    private lateinit var artistView: TextView
    private lateinit var playButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var searchBox: EditText
    private lateinit var libraryCount: TextView

    private val songs = mutableListOf<Song>()
    private var currentSong: Song? = null
    private var player: MediaPlayer? = null
    private var userSeeking = false
    private var lyricLines: List<LyricLine> = emptyList()
    private var currentLyricIndex = -1
    private val handler = Handler(Looper.getMainLooper())

    private val progressTask = object : Runnable {
        override fun run() {
            val mp = player
            if (mp != null) {
                try {
                    if (mp.isPlaying) {
                        if (!userSeeking && mp.duration > 0) seekBar.progress = mp.currentPosition
                        updateLyrics(mp.currentPosition.toLong())
                    }
                } catch (_: IllegalStateException) { }
            }
            handler.postDelayed(this, 350)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildScreen()
        handler.post(progressTask)
        if (hasAudioPermission()) loadSongs() else requestAudioPermission()
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        requestPermissions(arrayOf(permission), 41)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            Toast.makeText(this, "צריך לאשר גישה לקובצי המוזיקה כדי לסרוק את המכשיר", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(32))
        }
        scroll.addView(page)
        setContentView(scroll)

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = text("NAGAN", 27, white, true)
        brand.letterSpacing = 0.12f
        brandRow.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        brandRow.addView(text("OFFLINE MUSIC", 10, muted, true))
        page.addView(brandRow)
        page.addView(text("המוזיקה שלך. המילים שלך. בלי אינטרנט.", 14, muted, false).apply {
            setPadding(0, dp(5), 0, dp(18))
        })

        searchBox = EditText(this).apply {
            hint = "חיפוש שיר או זמר"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            isSingleLine = true
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(panel, 16)
        }
        page.addView(searchBox, LinearLayout.LayoutParams(-1, dp(50)))
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderSongs(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) {}
        })

        page.addView(sectionTitle("מתנגן עכשיו"))
        val playerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, 22)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        val cover = TextView(this).apply {
            text = "♫"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(white)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(accent, Color.rgb(40, 190, 180))).apply { cornerRadius = dp(18).toFloat() }
        }
        playerCard.addView(cover, LinearLayout.LayoutParams(-1, dp(112)))
        titleView = text("בחר שיר מהספרייה", 20, white, true).apply { setPadding(0, dp(15), 0, dp(3)) }
        artistView = text("המוזיקה המקומית שלך תופיע כאן", 13, muted, false)
        playerCard.addView(titleView)
        playerCard.addView(artistView)
        seekBar = SeekBar(this).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList = android.content.res.ColorStateList.valueOf(white)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(65, 70, 88))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) { userSeeking = true }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    userSeeking = false
                    try { player?.seekTo(seekBar?.progress ?: 0) } catch (_: Exception) { }
                }
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            })
        }
        playerCard.addView(seekBar, LinearLayout.LayoutParams(-1, dp(34)))
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        val prev = actionButton("⏮", false) { playAdjacent(-1) }
        playButton = actionButton("▶", true) { togglePlayback() }
        val next = actionButton("⏭", false) { playAdjacent(1) }
        controls.addView(prev)
        controls.addView(playButton, LinearLayout.LayoutParams(dp(76), dp(56)).apply { setMargins(dp(18), 0, dp(18), 0) })
        controls.addView(next)
        playerCard.addView(controls)
        page.addView(playerCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        val lyricsHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        lyricsHeader.addView(sectionTitle("מילות השיר"), LinearLayout.LayoutParams(0, -2, 1f))
        lyricsHeader.addView(actionButton("＋ הוספת מילים", false) { editLyrics() })
        page.addView(lyricsHeader)
        val lyricsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.rgb(17, 20, 31), 20)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        lyricsView = text("כאן יופיעו מילות השיר.\n\nאפשר להוסיף מילים משלך — הן יישמרו במכשיר ויהיו זמינות גם באופליין.", 16, muted, false).apply {
            gravity = Gravity.CENTER
            setLineSpacing(dp(8).toFloat(), 1f)
        }
        lyricsView.minHeight = dp(190)
        lyricsCard.addView(lyricsView, LinearLayout.LayoutParams(-1, -2))
        page.addView(lyricsCard)

        val libraryHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        libraryHeader.addView(sectionTitle("הספרייה שלי"), LinearLayout.LayoutParams(0, -2, 1f))
        libraryCount = text("0 שירים", 12, muted, false)
        libraryHeader.addView(libraryCount)
        page.addView(libraryHeader)
        songList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(songList)

        val aboutButton = actionButton("ⓘ  אודות NAGAN", false) { showAbout() }
        page.addView(aboutButton, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(22)
        })
    }

    private fun loadSongs() {
        songs.clear()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sort)
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    songs.add(Song(id, c.getString(titleCol) ?: "ללא שם", c.getString(artistCol) ?: "אמן לא ידוע",
                        c.getString(albumCol) ?: "", c.getLong(durationCol), uri))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "לא הצלחנו לסרוק את המוזיקה: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
        libraryCount.text = "${songs.size} שירים"
        renderSongs(searchBox.text.toString())
    }

    private fun renderSongs(filter: String) {
        if (!::songList.isInitialized) return
        songList.removeAllViews()
        val q = filter.trim().lowercase(Locale.getDefault())
        val matches = songs.filter { q.isEmpty() || it.title.lowercase(Locale.getDefault()).contains(q) || it.artist.lowercase(Locale.getDefault()).contains(q) }
        if (matches.isEmpty()) {
            songList.addView(text(if (songs.isEmpty()) "לא נמצאו שירים במכשיר. הוסף קובצי מוזיקה ואשר הרשאה לסריקה." else "לא נמצאו תוצאות לחיפוש.", 14, muted, false).apply {
                setPadding(dp(4), dp(12), dp(4), dp(12))
            })
            return
        }
        matches.take(250).forEach { song ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = rounded(panel, 14)
                isClickable = true
                isFocusable = true
                setOnClickListener { playSong(song) }
            }
            val icon = text("♫", 22, accent, true).apply { gravity = Gravity.CENTER }
            row.addView(icon, LinearLayout.LayoutParams(dp(42), dp(42)))
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            details.addView(text(song.title, 14, white, true))
            details.addView(text(song.artist, 12, muted, false).apply { setPadding(0, dp(3), 0, 0) })
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8); rightMargin = dp(8) })
            row.addView(text("▶", 14, muted, true))
            songList.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        }
    }

    private fun playSong(song: Song) {
        releasePlayer()
        currentSong = song
        titleView.text = song.title
        artistView.text = song.artist
        seekBar.progress = 0
        seekBar.max = song.duration.coerceAtLeast(1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        playButton.text = "…"
        loadLyrics(song)
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(this, song.uri)
            mp.setOnPreparedListener {
                if (player === mp) {
                    mp.start()
                    playButton.text = "Ⅱ"
                    updateLyrics(0)
                }
            }
            mp.setOnCompletionListener {
                playButton.text = "▶"
                playAdjacent(1)
            }
            mp.setOnErrorListener { _, _, _ ->
                playButton.text = "▶"
                Toast.makeText(this, "לא ניתן לנגן את הקובץ הזה", Toast.LENGTH_SHORT).show()
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            playButton.text = "▶"
            Toast.makeText(this, "שגיאה בפתיחת השיר", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePlayback() {
        val mp = player
        if (mp == null) {
            currentSong?.let { playSong(it) } ?: songs.firstOrNull()?.let { playSong(it) }
            return
        }
        try {
            if (mp.isPlaying) { mp.pause(); playButton.text = "▶" }
            else { mp.start(); playButton.text = "Ⅱ" }
        } catch (_: Exception) { }
    }

    private fun playAdjacent(direction: Int) {
        val current = currentSong ?: run { songs.firstOrNull()?.let { playSong(it) }; return }
        val index = songs.indexOfFirst { it.id == current.id }
        if (index < 0 || songs.isEmpty()) return
        val next = (index + direction + songs.size) % songs.size
        playSong(songs[next])
    }

    private fun loadLyrics(song: Song) {
        val prefsKey = "lyrics_${song.id}"
        val saved = getSharedPreferences("nagan_lyrics", MODE_PRIVATE).getString(prefsKey, null)
        val source = saved ?: bundledLyrics(song)
        lyricLines = parseLyrics(source.orEmpty())
        currentLyricIndex = -1
        if (source.isNullOrBlank()) {
            lyricsView.text = "המילים לשיר הזה עדיין לא נמצאות במאגר.\n\nאפשר להוסיף אותן בלחיצה על ״הוספת מילים״. המילים יישמרו בטלפון."
            lyricsView.setTextColor(muted)
            lyricsView.gravity = Gravity.CENTER
        } else {
            lyricsView.gravity = Gravity.CENTER
            updateLyrics(0)
        }
    }

    private fun bundledLyrics(song: Song): String? {
        return try {
            val root = JSONObject(assets.open("lyrics.json").bufferedReader().use { it.readText() })
            val entries = root.optJSONObject("songs") ?: return null
            val titleKey = normalizeLyricsKey(song.title)
            val artistKey = normalizeLyricsKey(song.artist)
            val exactKey = "$titleKey|$artistKey"
            entries.optString(exactKey).takeIf { it.isNotBlank() }?.let { return it }

            // Fallback to title-only matching: Android music tags often use different artist names.
            val keys = entries.keys()
            while (keys.hasNext()) {
                val candidateKey = keys.next()
                val candidateTitle = normalizeLyricsKey(candidateKey.substringBefore("|"))
                if (candidateTitle == titleKey) {
                    entries.optString(candidateKey).takeIf { it.isNotBlank() }?.let { return it }
                }
            }
            null
        } catch (_: Exception) { null }
    }

    private fun normalizeLyricsKey(value: String): String {
        val normalized = java.text.Normalizer.normalize(value.trim().lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
    }

    private fun parseLyrics(raw: String): List<LyricLine> {
        if (raw.isBlank()) return emptyList()
        val result = mutableListOf<LyricLine>()
        val pattern = Regex("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{1,3}))?]")
        raw.lines().forEach { line ->
            val matches = pattern.findAll(line).toList()
            val lyric = pattern.replace(line, "").trim()
            if (matches.isEmpty()) {
                if (lyric.isNotBlank()) result.add(LyricLine(-1, lyric))
            } else {
                matches.forEach { m ->
                    val minutes = m.groupValues[1].toLong()
                    val seconds = m.groupValues[2].toLong()
                    val fraction = m.groupValues[3].padEnd(3, '0').take(3).ifBlank { "000" }.toLong()
                    result.add(LyricLine((minutes * 60 + seconds) * 1000 + fraction, lyric))
                }
            }
        }
        return result.sortedBy { if (it.timeMs < 0) Long.MAX_VALUE else it.timeMs }
    }

    private fun updateLyrics(position: Long) {
        if (lyricLines.isEmpty()) return
        val timed = lyricLines.any { it.timeMs >= 0 }
        if (!timed) {
            if (currentLyricIndex == -1) {
                lyricsView.text = lyricLines.joinToString("\n\n") { it.text }
                lyricsView.setTextColor(white)
                lyricsView.gravity = Gravity.CENTER
                currentLyricIndex = 0
            }
            return
        }
        val active = lyricLines.indexOfLast { it.timeMs >= 0 && it.timeMs <= position }
        if (active == currentLyricIndex) return
        currentLyricIndex = active
        val display = SpannableStringBuilder()
        lyricLines.forEachIndexed { index, line ->
            val start = display.length
            display.append(line.text)
            val end = display.length
            if (index == active) {
                display.setSpan(ForegroundColorSpan(white), start, end, 0)
                display.setSpan(StyleSpan(Typeface.BOLD), start, end, 0)
            } else {
                display.setSpan(ForegroundColorSpan(if (index < active) muted else Color.rgb(105, 111, 132)), start, end, 0)
            }
            if (index != lyricLines.lastIndex) display.append("\n\n")
        }
        lyricsView.text = display
        lyricsView.gravity = Gravity.CENTER
    }

    private fun editLyrics() {
        val song = currentSong
        if (song == null) {
            Toast.makeText(this, "קודם בחר שיר מהספרייה", Toast.LENGTH_SHORT).show()
            return
        }
        val editor = EditText(this).apply {
            setTextColor(white)
            setHintTextColor(muted)
            hint = "הדבק כאן מילים רגילות או בפורמט LRC עם חותמות זמן, למשל [00:12.50]"
            setText(getSharedPreferences("nagan_lyrics", MODE_PRIVATE).getString("lyrics_${song.id}", bundledLyrics(song)).orEmpty())
            minLines = 7
            maxLines = 14
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(panel, 12)
        }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        container.addView(editor, LinearLayout.LayoutParams(-1, dp(220)))
        AlertDialog.Builder(this)
            .setTitle("מילים: ${song.title}")
            .setView(container)
            .setNegativeButton("ביטול", null)
            .setNeutralButton("מחיקת מילים") { _, _ ->
                getSharedPreferences("nagan_lyrics", MODE_PRIVATE).edit().remove("lyrics_${song.id}").apply()
                loadLyrics(song)
            }
            .setPositiveButton("שמירה במכשיר") { _, _ ->
                getSharedPreferences("nagan_lyrics", MODE_PRIVATE).edit().putString("lyrics_${song.id}", editor.text.toString()).apply()
                loadLyrics(song)
                Toast.makeText(this, "המילים נשמרו אופליין", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun releasePlayer() {
        try { player?.stop() } catch (_: Exception) { }
        try { player?.release() } catch (_: Exception) { }
        player = null
    }

    private fun showAbout() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(18), dp(24), dp(8))
        }

        val logo = TextView(this).apply {
            text = "M.K."
            textSize = 25f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(accent, Color.rgb(34, 190, 176))
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), Color.rgb(225, 228, 255))
            }
        }
        content.addView(logo, LinearLayout.LayoutParams(dp(92), dp(92)))
        content.addView(text("NAGAN", 25, white, true).apply {
            letterSpacing = 0.14f
            setPadding(0, dp(14), 0, dp(6))
        })
        content.addView(text("נגן מוזיקה אופליין", 14, muted, false))
        content.addView(text("נבנה על ידי M.K.\nכל הזכויות שמורות", 16, white, true).apply {
            gravity = Gravity.CENTER
            setLineSpacing(dp(5).toFloat(), 1f)
            setPadding(0, dp(22), 0, dp(8))
        })

        AlertDialog.Builder(this)
            .setView(content)
            .setPositiveButton("סגירה", null)
            .show()
    }

    private fun sectionTitle(label: String): TextView =
        text(label, 18, white, true).apply { setPadding(0, dp(22), 0, dp(10)) }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun actionButton(label: String, primary: Boolean, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = if (primary) 19f else 12f
            isAllCaps = false
            setTextColor(white)
            background = rounded(if (primary) accent else Color.rgb(43, 48, 66), 18)
            setPadding(dp(12), dp(5), dp(12), dp(5))
            setOnClickListener { action() }
        }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(progressTask)
        releasePlayer()
        super.onDestroy()
    }
}
