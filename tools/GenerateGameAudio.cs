// One-shot binary WAV generator for Bichinho Virtual.
// Writes PCM 16-bit LE WAV only (no text/UTF-8 paths for sample data).
using System;
using System.IO;

namespace BichinhoAudio {
  static class Program {
    const int SR = 44100;

    static void Main(string[] args) {
      string outDir = args.Length > 0 ? args[0] : @"app\src\main\res\raw";
      Directory.CreateDirectory(outDir);

      // BGM stereo
      WriteStereo(Path.Combine(outDir, "bgm_home.wav"), MakeHome(52.0));
      WriteStereo(Path.Combine(outDir, "bgm_shop.wav"), MakeShop(48.0));
      WriteStereo(Path.Combine(outDir, "bgm_minigame.wav"), MakeMinigame(46.0));
      WriteStereo(Path.Combine(outDir, "bgm_celebration.wav"), MakeCelebration(8.0));
      WriteStereo(Path.Combine(outDir, "bgm_incubator.wav"), MakeIncubator(50.0));

      // SFX mono
      WriteMono(Path.Combine(outDir, "sfx_button.wav"), MakeClick(0.08, 880));
      WriteMono(Path.Combine(outDir, "sfx_tap.wav"), MakeClick(0.05, 1200));
      WriteMono(Path.Combine(outDir, "sfx_buy.wav"), MakeCoinish(0.35, true));
      WriteMono(Path.Combine(outDir, "sfx_coin.wav"), MakeCoinish(0.28, false));
      WriteMono(Path.Combine(outDir, "sfx_feed.wav"), MakeMunch(0.40));
      WriteMono(Path.Combine(outDir, "sfx_drink.wav"), MakeDrink(0.45));
      WriteMono(Path.Combine(outDir, "sfx_bath.wav"), MakeBubbles(0.70));
      WriteMono(Path.Combine(outDir, "sfx_sleep.wav"), MakeSoftTone(0.90, 220, 0.35));
      WriteMono(Path.Combine(outDir, "sfx_wakeup.wav"), MakeWake(0.55));
      WriteMono(Path.Combine(outDir, "sfx_play.wav"), MakePlay(0.40));
      WriteMono(Path.Combine(outDir, "sfx_pet_happy.wav"), MakeHappy(0.45));
      WriteMono(Path.Combine(outDir, "sfx_pet_sad.wav"), MakeSad(0.55));
      WriteMono(Path.Combine(outDir, "sfx_pet_sick.wav"), MakeSick(0.60));
      WriteMono(Path.Combine(outDir, "sfx_level_up.wav"), MakeFanfare(0.85));
      WriteMono(Path.Combine(outDir, "sfx_mission.wav"), MakeChime(0.50, 523.25, 659.25, 783.99));
      WriteMono(Path.Combine(outDir, "sfx_achievement.wav"), MakeChime(0.70, 392.00, 523.25, 659.25, 783.99));
      WriteMono(Path.Combine(outDir, "sfx_birth.wav"), MakeBirth(1.20));
      WriteMono(Path.Combine(outDir, "sfx_egg_crack.wav"), MakeCrack(0.35));
      WriteMono(Path.Combine(outDir, "sfx_evolution.wav"), MakeEvolution(1.40));
      WriteMono(Path.Combine(outDir, "sfx_yawn.wav"), MakeYawn(0.80));

      Console.WriteLine("OK generated wavs into " + Path.GetFullPath(outDir));
    }

    // ---------- music builders ----------
    static float[] MakeHome(double sec) {
      // Soft C-major Tamagotchi pad + gentle arpeggio, loopable
      int n = (int)(sec * SR);
      float[] L = new float[n], R = new float[n];
      int[] arp = { 60, 64, 67, 72, 67, 64 }; // C E G C5 G E
      double bpm = 88.0;
      double beat = 60.0 / bpm;
      var rnd = new Random(11);
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double envPad = 0.18 + 0.07 * Math.Sin(2 * Math.PI * t / 8.0);
        double pad =
          Tri(Midi(48), t) * 0.22 +
          Sine(Midi(55), t) * 0.18 +
          Sine(Midi(60), t) * 0.12;
        int step = (int)(t / (beat / 2.0)) % arp.Length;
        double noteT = t % (beat / 2.0);
        double aEnv = Math.Min(1.0, noteT * 18.0) * Math.Exp(-noteT * 3.2);
        double mel = SoftSquare(Midi(arp[step]), t) * 0.16 * aEnv;
        double softNoise = ((rnd.NextDouble() * 2 - 1) * 0.008) * envPad;
        double sample = (pad + mel + softNoise) * 0.9;
        double pan = 0.5 + 0.12 * Math.Sin(2 * Math.PI * t / 6.0);
        L[i] = (float)(sample * (1.0 - pan) * 2.0 * 0.55);
        R[i] = (float)(sample * pan * 2.0 * 0.55);
      }
      CrossfadeLoop(L, R, (int)(0.35 * SR));
      return Interleave(L, R);
    }

    static float[] MakeShop(double sec) {
      int n = (int)(sec * SR);
      float[] L = new float[n], R = new float[n];
      int[] arp = { 67, 71, 74, 79, 74, 71, 67, 64 }; // G major bounce
      double bpm = 112.0;
      double beat = 60.0 / bpm;
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        int step = (int)(t / (beat / 2.0)) % arp.Length;
        double noteT = t % (beat / 2.0);
        double aEnv = Math.Min(1.0, noteT * 25.0) * Math.Exp(-noteT * 4.0);
        double bassT = t % beat;
        double bEnv = Math.Min(1.0, bassT * 20.0) * Math.Exp(-bassT * 5.0);
        double mel = SoftSquare(Midi(arp[step]), t) * 0.20 * aEnv;
        double bass = Tri(Midi(43 + (step % 2 == 0 ? 0 : 5)), t) * 0.18 * bEnv;
        double sparkle = Sine(Midi(arp[step] + 12), t) * 0.08 * aEnv;
        double sample = mel + bass + sparkle;
        double pan = 0.5 + 0.2 * Math.Sin(2 * Math.PI * t * 0.4);
        L[i] = (float)(sample * (1.2 - pan) * 0.55);
        R[i] = (float)(sample * (0.8 + pan) * 0.55);
      }
      CrossfadeLoop(L, R, (int)(0.25 * SR));
      return Interleave(L, R);
    }

    static float[] MakeMinigame(double sec) {
      int n = (int)(sec * SR);
      float[] L = new float[n], R = new float[n];
      int[] arp = { 64, 67, 71, 74, 76, 74, 71, 67 };
      double bpm = 136.0;
      double beat = 60.0 / bpm;
      var rnd = new Random(42);
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        int step = (int)(t / (beat / 2.0)) % arp.Length;
        double noteT = t % (beat / 2.0);
        double aEnv = Math.Min(1.0, noteT * 40.0) * Math.Exp(-noteT * 6.0);
        double kickT = t % beat;
        double kick = Math.Exp(-kickT * 18.0) * Sine(80 + 40 * Math.Exp(-kickT * 30), t) * 0.35;
        double hatT = t % (beat / 2.0);
        double hat = Math.Exp(-hatT * 50.0) * ((rnd.NextDouble() * 2 - 1) * 0.08);
        double mel = SoftSquare(Midi(arp[step]), t) * 0.22 * aEnv;
        double sample = mel + kick + hat;
        L[i] = (float)(sample * 0.58);
        R[i] = (float)(sample * 0.58 * (0.9 + 0.1 * Math.Sin(t * 9)));
      }
      CrossfadeLoop(L, R, (int)(0.2 * SR));
      return Interleave(L, R);
    }

    static float[] MakeCelebration(double sec) {
      int n = (int)(sec * SR);
      float[] L = new float[n], R = new float[n];
      int[] notes = { 60, 64, 67, 72, 76, 79, 84 };
      double stepDur = sec / notes.Length;
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        int idx = Math.Min(notes.Length - 1, (int)(t / stepDur));
        double local = t - idx * stepDur;
        double env = Math.Min(1.0, local * 30.0) * Math.Exp(-local * 2.5);
        double s =
          SoftSquare(Midi(notes[idx]), t) * 0.28 +
          Sine(Midi(notes[idx] + 7), t) * 0.16 +
          Tri(Midi(notes[Math.Max(0, idx - 1)]), t) * 0.10;
        s *= env;
        L[i] = (float)(s * 0.65);
        R[i] = (float)(s * 0.65);
      }
      return Interleave(L, R);
    }

    static float[] MakeIncubator(double sec) {
      int n = (int)(sec * SR);
      float[] L = new float[n], R = new float[n];
      var rnd = new Random(7);
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double pad =
          Sine(Midi(57), t) * 0.12 +
          Sine(Midi(60) + 0.15 * Math.Sin(t * 0.4), t) * 0.14 +
          Sine(Midi(64), t) * 0.10;
        // soft bell every ~1.5s
        double bellPhase = t % 1.5;
        double bEnv = Math.Exp(-bellPhase * 2.8) * Math.Min(1.0, bellPhase * 40.0);
        double bell = (Sine(Midi(79), t) + 0.4 * Sine(Midi(84), t)) * 0.18 * bEnv;
        double shimmer = ((rnd.NextDouble() * 2 - 1) * 0.01) * (0.5 + 0.5 * Math.Sin(t * 0.7));
        double sample = (pad + bell + shimmer) * 0.85;
        double pan = 0.5 + 0.25 * Math.Sin(t * 0.35);
        L[i] = (float)(sample * (1.0 - pan) * 1.3 * 0.5);
        R[i] = (float)(sample * pan * 1.3 * 0.5);
      }
      CrossfadeLoop(L, R, (int)(0.4 * SR));
      return Interleave(L, R);
    }

    // ---------- sfx ----------
    static float[] MakeClick(double sec, double hz) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double env = Math.Exp(-t * 45.0);
        s[i] = (float)(SoftSquare(hz, t) * env * 0.55);
      }
      return s;
    }

    static float[] MakeCoinish(double sec, bool richer) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      double[] freqs = richer ? new double[] { 987.77, 1318.51, 1567.98 } : new double[] { 1046.5, 1318.51 };
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double v = 0;
        for (int k = 0; k < freqs.Length; k++) {
          double local = Math.Max(0, t - k * 0.06);
          double env = Math.Exp(-local * 10.0) * Math.Min(1.0, local * 80.0 + 0.001);
          v += Sine(freqs[k], t) * env * (0.35 - k * 0.05);
        }
        s[i] = (float)Clamp(v, -1, 1);
      }
      return s;
    }

    static float[] MakeMunch(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      var rnd = new Random(3);
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double burst = 0;
        for (int b = 0; b < 4; b++) {
          double local = t - b * 0.08;
          if (local < 0) continue;
          double env = Math.Exp(-local * 22.0);
          burst += ((rnd.NextDouble() * 2 - 1) * 0.4 + SoftSquare(180 + b * 40, t) * 0.3) * env;
        }
        s[i] = (float)Clamp(burst * 0.7, -1, 1);
      }
      return s;
    }

    static float[] MakeDrink(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double f = 400 + 500 * t;
        double env = Math.Min(1.0, t * 8.0) * Math.Exp(-t * 3.5);
        s[i] = (float)(Sine(f, t) * env * 0.45 + SoftSquare(f * 0.5, t) * env * 0.1);
      }
      return s;
    }

    static float[] MakeBubbles(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      var rnd = new Random(9);
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double v = 0;
        for (int b = 0; b < 8; b++) {
          double start = b * 0.07 + 0.02 * rnd.NextDouble();
          double local = t - start;
          if (local < 0 || local > 0.25) continue;
          double f = 600 + rnd.Next(0, 900);
          double env = Math.Exp(-local * 18.0) * Math.Min(1.0, local * 60.0);
          v += Sine(f, t) * env * 0.25;
        }
        s[i] = (float)Clamp(v, -1, 1);
      }
      return s;
    }

    static float[] MakeSoftTone(double sec, double hz, double amp) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double env = Math.Min(1.0, t * 3.0) * Math.Exp(-t * 1.8);
        s[i] = (float)((Sine(hz, t) + 0.3 * Sine(hz * 2, t)) * env * amp);
      }
      return s;
    }

    static float[] MakeWake(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      int[] notes = { 60, 64, 67, 72 };
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        int idx = Math.Min(notes.Length - 1, (int)(t / (sec / notes.Length)));
        double local = t - idx * (sec / notes.Length);
        double env = Math.Min(1.0, local * 25.0) * Math.Exp(-local * 4.0);
        s[i] = (float)(SoftSquare(Midi(notes[idx]), t) * env * 0.4);
      }
      return s;
    }

    static float[] MakePlay(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double env = Math.Exp(-t * 6.0);
        s[i] = (float)((SoftSquare(523.25, t) + SoftSquare(659.25, t) * 0.7) * env * 0.35);
      }
      return s;
    }

    static float[] MakeHappy(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double f = 500 + 900 * t;
        double env = Math.Min(1.0, t * 20.0) * Math.Exp(-t * 5.0);
        s[i] = (float)(SoftSquare(f, t) * env * 0.4);
      }
      return s;
    }

    static float[] MakeSad(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double f = 420 - 180 * Math.Min(1.0, t / sec);
        double env = Math.Min(1.0, t * 6.0) * Math.Exp(-t * 2.2);
        s[i] = (float)((Sine(f, t) + 0.2 * Sine(f * 0.5, t)) * env * 0.4);
      }
      return s;
    }

    static float[] MakeSick(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      var rnd = new Random(5);
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double wobble = 180 + 40 * Math.Sin(2 * Math.PI * 6 * t);
        double env = Math.Min(1.0, t * 5.0) * Math.Exp(-t * 2.0);
        s[i] = (float)((Sine(wobble, t) * 0.35 + (rnd.NextDouble() * 2 - 1) * 0.05) * env);
      }
      return s;
    }

    static float[] MakeFanfare(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      int[] notes = { 67, 71, 74, 79, 86 };
      double step = sec / notes.Length;
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        int idx = Math.Min(notes.Length - 1, (int)(t / step));
        double local = t - idx * step;
        double env = Math.Min(1.0, local * 40.0) * Math.Exp(-local * 3.5);
        s[i] = (float)((SoftSquare(Midi(notes[idx]), t) + Sine(Midi(notes[idx] + 12), t) * 0.3) * env * 0.4);
      }
      return s;
    }

    static float[] MakeChime(double sec, params double[] freqs) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double v = 0;
        for (int k = 0; k < freqs.Length; k++) {
          double local = Math.Max(0, t - k * 0.08);
          double env = Math.Exp(-local * 5.0) * Math.Min(1.0, local * 50.0 + 0.001);
          v += Sine(freqs[k], t) * env * 0.3;
        }
        s[i] = (float)Clamp(v, -1, 1);
      }
      return s;
    }

    static float[] MakeBirth(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double env = Math.Min(1.0, t * 4.0) * Math.Exp(-(t - 0.3) * (t > 0.3 ? 1.5 : 0));
        if (t < 0.3) env = Math.Min(1.0, t * 8.0);
        double sparkle = Sine(880 + 400 * Math.Sin(t * 8), t) * 0.2;
        double pad = Sine(Midi(72), t) * 0.25 + Sine(Midi(76), t) * 0.18;
        s[i] = (float)Clamp((pad + sparkle) * env * 0.7, -1, 1);
      }
      return s;
    }

    static float[] MakeCrack(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      var rnd = new Random(13);
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double env = Math.Exp(-t * 25.0);
        double noise = (rnd.NextDouble() * 2 - 1);
        double click = SoftSquare(1200 * Math.Exp(-t * 10), t);
        s[i] = (float)Clamp((noise * 0.55 + click * 0.35) * env, -1, 1);
      }
      return s;
    }

    static float[] MakeEvolution(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double rise = 300 + 900 * (t / sec);
        double env = Math.Min(1.0, t * 3.0) * (0.4 + 0.6 * (t / sec));
        double shimmer = Sine(rise * 2, t) * 0.15 * Math.Sin(t * 20);
        s[i] = (float)Clamp((SoftSquare(rise, t) * 0.25 + Sine(rise * 1.5, t) * 0.2 + shimmer) * env, -1, 1);
      }
      return s;
    }

    static float[] MakeYawn(double sec) {
      int n = (int)(sec * SR);
      float[] s = new float[n];
      for (int i = 0; i < n; i++) {
        double t = i / (double)SR;
        double f = 280 - 80 * Math.Sin(Math.PI * Math.Min(1.0, t / sec));
        double env = Math.Min(1.0, t * 2.5) * Math.Exp(-Math.Max(0, t - 0.35) * 2.0);
        s[i] = (float)((Sine(f, t) + 0.25 * Sine(f * 0.5, t)) * env * 0.4);
      }
      return s;
    }

    // ---------- synth helpers ----------
    static double Midi(int note) { return 440.0 * Math.Pow(2.0, (note - 69) / 12.0); }
    static double Sine(double hz, double t) { return Math.Sin(2 * Math.PI * hz * t); }
    static double Tri(double hz, double t) {
      double p = (t * hz) % 1.0;
      return p < 0.5 ? (p * 4 - 1) : (3 - p * 4);
    }
    static double SoftSquare(double hz, double t) {
      // band-limited-ish soft square via tanh of sine
      return Math.Tanh(2.2 * Sine(hz, t));
    }
    static double Clamp(double v, double a, double b) { return v < a ? a : (v > b ? b : v); }

    static void CrossfadeLoop(float[] L, float[] R, int fade) {
      if (fade <= 0 || fade * 2 >= L.Length) return;
      for (int i = 0; i < fade; i++) {
        float w = i / (float)fade;
        int end = L.Length - fade + i;
        float a = 1f - w;
        float b = w;
        float l = L[end] * a + L[i] * b;
        float r = R[end] * a + R[i] * b;
        L[end] = l; R[end] = r;
        L[i] = l; R[i] = r;
      }
    }

    static float[] Interleave(float[] L, float[] R) {
      float[] o = new float[L.Length * 2];
      for (int i = 0; i < L.Length; i++) { o[i * 2] = L[i]; o[i * 2 + 1] = R[i]; }
      return o;
    }

    static void WriteMono(string path, float[] mono) { WriteWav(path, mono, 1); }
    static void WriteStereo(string path, float[] interleaved) { WriteWav(path, interleaved, 2); }

    static void WriteWav(string path, float[] samples, int channels) {
      int dataBytes = samples.Length * 2;
      using (var fs = new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.None))
      using (var bw = new BinaryWriter(fs)) {
        bw.Write(new byte[] { (byte)'R', (byte)'I', (byte)'F', (byte)'F' });
        bw.Write(36 + dataBytes);
        bw.Write(new byte[] { (byte)'W', (byte)'A', (byte)'V', (byte)'E' });
        bw.Write(new byte[] { (byte)'f', (byte)'m', (byte)'t', (byte)' ' });
        bw.Write(16);
        bw.Write((short)1); // PCM
        bw.Write((short)channels);
        bw.Write(SR);
        bw.Write(SR * channels * 2); // byte rate
        bw.Write((short)(channels * 2)); // block align
        bw.Write((short)16); // bits
        bw.Write(new byte[] { (byte)'d', (byte)'a', (byte)'t', (byte)'a' });
        bw.Write(dataBytes);
        for (int i = 0; i < samples.Length; i++) {
          double v = Clamp(samples[i], -1.0, 1.0);
          short s = (short)Math.Round(v * 32767.0);
          bw.Write(s);
        }
      }
    }
  }
}
