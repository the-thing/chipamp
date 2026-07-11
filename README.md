# 🎵 Chipamp

[![Java CI](https://github.com/the-thing/chipamp/actions/workflows/ci.yml/badge.svg)](https://github.com/the-thing/chipamp/actions/workflows/ci.yml)
[![CodeQL](https://github.com/the-thing/chipamp/actions/workflows/codeql.yml/badge.svg)](https://github.com/the-thing/chipamp/actions/workflows/codeql.yml)
[![Codecov](https://codecov.io/github/the-thing/chipamp/branch/main/graph/badge.svg)](https://app.codecov.io/github/the-thing/chipamp)

**A pure-Java ProTracker MOD file player and playback engine.**

Chipamp parses and plays classic Amiga (and PC) `.mod` tracker files with sample-accurate, tick-based emulation of the
original ProTracker / OpenMPT playback behavior — arpeggios, portamento, vibrato, the Amiga hardware low-pass filter,
and all the other quirks that make old-school chiptunes sound the way they do.

No native dependencies, no external libraries — just Java and `javax.sound.sampled`.

## 🧩 Features

- **Full ProTracker MOD parser** — reads 15-sample and 31-sample module formats, sample headers, pattern data, and the
  song / pattern sequence table
- **Smart tracker & channel detection** — recognizes tracker ID signatures (`M.K.`, `M!K!`, `FLT4/8`, `6CHN`, `TDZ4`,
  `OCTA`/`OKTA`, and more) to infer channel count (1–16 channels) and sample-count layout
- **Cycle-accurate playback engine** — tick / row-based sampler that mirrors real ProTracker timing (speed, tempo,
  CIA tick calculation)
- **Every classic effect implemented** — see the full effect tables below
- **OpenMPT-style sample correction** — automatically fixes malformed loop points found in real-world `.mod` files
- **Fast seeking** — pre-computed per-row channel / context snapshots let you jump to any sequence position or row
  instantly, without replaying from the start
- **Infinite loop detection** — safely detects and breaks out of modules that loop forever
- **Runtime control** — mute / unmute individual channels, toggle individual effects or extended effects on/off, adjust
  panning, clock rate (PAL / NTSC), sampling rate, volume, and more — all live
- **Flexible audio output** — render to a byte buffer, WAV-style PCM, or stream directly through `javax.sound.sampled`
  with a built-in async, non-blocking `SourceDataLine` writer---

## 🎹 Supported Effects

### Basic Effects

| Code | Effect                         | Description                                               |
|------|--------------------------------|-----------------------------------------------------------|
| `0`  | Arpeggio                       | Rapidly cycles between the base note and two offset notes |
| `1`  | Slide Up                       | Slides the pitch up                                       |
| `2`  | Slide Down                     | Slides the pitch down                                     |
| `3`  | Tone Portamento                | Glides smoothly toward a target note                      |
| `4`  | Vibrato                        | Oscillates pitch using a configurable waveform            |
| `5`  | Tone Portamento + Volume Slide | Combines effects `3` and `A`                              |
| `6`  | Vibrato + Volume Slide         | Combines effects `4` and `A`                              |
| `7`  | Tremolo                        | Oscillates volume using a configurable waveform           |
| `8`  | Set Panning Position           | Sets fine stereo panning                                  |
| `9`  | Set Sample Offset              | Starts sample playback from an offset                     |
| `A`  | Volume Slide                   | Slides volume up or down                                  |
| `B`  | Position Jump                  | Jumps to another position in the song sequence            |
| `C`  | Set Volume                     | Sets channel volume directly                              |
| `D`  | Pattern Break                  | Jumps to a specific row in the next pattern               |
| `E`  | Extended Effect                | Dispatches to one of the extended (`Ex`) effects below    |
| `F`  | Set Speed / Tempo              | Sets ticks-per-row or beats-per-minute                    |

### Extended Effects

| Code | Effect                 | Description                                        |
|------|------------------------|----------------------------------------------------|
| `E0` | Set Filter             | Toggles the Amiga hardware low-pass filter         |
| `E1` | Fine Slide Up          | Small one-time pitch slide up                      |
| `E2` | Fine Slide Down        | Small one-time pitch slide down                    |
| `E3` | Set Glissando          | Snaps tone portamento to the nearest semitone      |
| `E4` | Set Vibrato Waveform   | Sine, sawtooth, or square                          |
| `E5` | Set Fine Tune          | Overrides the sample's fine-tune value             |
| `E6` | Loop Pattern           | Loops a range of rows a set number of times        |
| `E7` | Set Tremolo Waveform   | Sine, sawtooth, or square                          |
| `E8` | Rough Panning          | Coarse 4-bit stereo panning                        |
| `E9` | Retrigger Sample       | Retriggers the sample every *n* ticks              |
| `EA` | Fine Volume Slide Up   | Small one-time volume increase                     |
| `EB` | Fine Volume Slide Down | Small one-time volume decrease                     |
| `EC` | Cut Sample             | Silences the channel after *n* ticks               |
| `ED` | Delay Sample           | Delays a note trigger by *n* ticks                 |
| `EE` | Delay Pattern          | Repeats the current row *n* times (pattern delay)  |
| `EF` | Invert Loop            | "Funk repeat" — inverts loop sample data over time |

## 🚀 Usage

### Load and play a module

```java
ModLoader loader = new ModLoader();
Mod mod = loader.load(new File("classic_tune.mod"));

Sampler sampler = new Sampler();
sampler.updateMod(mod);

Player player = new Player(sampler);
player.play();                                          // blocks until the module finishes
```

### Render to a byte buffer (e.g. for exporting)

```java
Sampler sampler = new Sampler();
sampler.updateMod(mod);

byte[] pcm = sampler.read();                            // full module rendered as 16-bit PCM
```

### Seek, mute, and tweak playback live

```java
sampler.seekSequence(4, 16);                            // jump to sequence position 4, row 16
sampler.setMuted(2, true);                              // mute channel 3
sampler.setEffectEnabled(EffectType.ARPEGGIO, false);
sampler.setClockHz(Mods.NTSC_CLOCK_HZ);                 // switch PAL -> NTSC pitch
sampler.setVolumeMultiplier(0.75f);
```

### Query module metadata

```java
System.out.println(mod.getTitle());
System.out.println(mod.getChannelCount() + " channels");
System.out.println(Mods.getUniqueEffects(mod));
System.out.println(sampler.getModLength(TimeUnit.SECONDS) + "s");
```

## 📦 Resources

[https://moddingwiki.shikadi.net/wiki/MOD_Format](https://moddingwiki.shikadi.net/wiki/MOD_Format)
<br/>
[https://www.aes.id.au/modformat.html](https://www.aes.id.au/modformat.html)
<br/>
[https://www.fileformat.info/format/mod/corion.htm](https://www.fileformat.info/format/mod/corion.htm)
<br/>
[https://www.exotica.org.uk/wiki/Protracker](https://www.exotica.org.uk/wiki/Protracker)
<br/>
[https://cdimage.debian.org/mirror/CRAN/web/packages/ProTrackR/refman/ProTrackR.html](https://cdimage.debian.org/mirror/CRAN/web/packages/ProTrackR/refman/ProTrackR.html)