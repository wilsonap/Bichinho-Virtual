import os
import glob
import struct
import hashlib

def audit_audio():
    files = sorted(glob.glob("app/src/main/res/raw/*.wav"))
    print(f"Total audio files in raw: {len(files)}\n")
    print(f"{'Filename':<22} | {'Size':<8} | {'RIFF?':<5} | {'Codec':<5} | {'Ch':<3} | {'SampleRate':<10} | {'Bits':<4} | {'Duration':<9} | {'EF BF BD':<8} | {'MD5 Hash'}")
    print("-" * 115)
    
    corrupted_count = 0
    valid_count = 0

    for filepath in files:
        fname = os.path.basename(filepath)
        size = os.path.getsize(filepath)
        with open(filepath, "rb") as f:
            data = f.read()

        md5 = hashlib.md5(data).hexdigest()
        ef_count = data.count(b"\xef\xbf\xbd")
        is_riff = len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WAVE"

        codec = "N/A"
        channels = "N/A"
        samplerate = "N/A"
        bitdepth = "N/A"
        duration = "N/A"
        valid_wav = False

        if is_riff:
            try:
                pos = 12
                total_len = len(data)
                while pos + 8 <= total_len:
                    chunk_id = data[pos:pos+4]
                    chunk_len = struct.unpack("<I", data[pos+4:pos+8])[0]
                    if chunk_id == b"fmt ":
                        fmt_data = data[pos+8:pos+8+chunk_len]
                        if len(fmt_data) >= 16:
                            audio_fmt, num_chan, srate, byterate, blockalign, bits = struct.unpack("<HHIIHH", fmt_data[:16])
                            codec = "PCM" if audio_fmt == 1 else str(audio_fmt)
                            channels = str(num_chan)
                            samplerate = str(srate)
                            bitdepth = str(bits)
                    elif chunk_id == b"data":
                        if samplerate != "N/A" and channels != "N/A" and bitdepth != "N/A":
                            bytes_per_sec = int(samplerate) * int(channels) * (int(bitdepth) // 8)
                            if bytes_per_sec > 0:
                                duration = f"{chunk_len / bytes_per_sec:.2f}s"
                                valid_wav = True
                    pos += 8 + chunk_len
            except Exception as e:
                valid_wav = False
        
        if valid_wav and ef_count == 0:
            valid_count += 1
        else:
            corrupted_count += 1

        print(f"{fname:<22} | {size:<8} | {str(is_riff):<5} | {codec:<5} | {channels:<3} | {samplerate:<10} | {bitdepth:<4} | {duration:<9} | {ef_count:<8} | {md5}")

    print("\n" + "=" * 115)
    print(f"Summary: Valid WAV files = {valid_count} / {len(files)} | Corrupted = {corrupted_count} / {len(files)}")

if __name__ == "__main__":
    audit_audio()
