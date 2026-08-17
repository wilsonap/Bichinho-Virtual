import os
import subprocess
import struct
import hashlib

def audit_git_history(repo_path="/tmp/clean_repo"):
    os.chdir(repo_path)
    commits_output = subprocess.check_output(["git", "log", "--reverse", "--format=%H %h %s"]).decode("utf-8")
    commits = [line.strip().split(" ", 2) for line in commits_output.strip().split("\n") if line.strip()]
    
    print(f"Total commits found: {len(commits)}")
    
    for full_hash, short_hash, subject in commits:
        print(f"\n========================================================")
        print(f"Commit: {short_hash} - {subject}")
        print(f"========================================================")
        
        # Check files at this commit
        ls_tree = subprocess.check_output(["git", "ls-tree", "-r", full_hash, "app/src/main/res/raw"]).decode("utf-8", errors="ignore")
        lines = [l for l in ls_tree.strip().split("\n") if l.endswith(".wav")]
        print(f"WAV files in commit: {len(lines)}")
        
        valid_wavs = 0
        corrupted_wavs = 0
        
        for line in lines:
            parts = line.split()
            if len(parts) < 4: continue
            obj_hash = parts[2]
            filepath = parts[3]
            fname = os.path.basename(filepath)
            
            # get blob data
            blob_data = subprocess.check_output(["git", "cat-file", "-p", obj_hash])
            ef_count = blob_data.count(b"\xef\xbf\xbd")
            is_riff = len(blob_data) >= 12 and blob_data[:4] == b"RIFF" and blob_data[8:12] == b"WAVE"
            
            samplerate = 0
            channels = 0
            bits = 0
            duration_str = "N/A"
            valid_wav = False
            
            if is_riff:
                try:
                    pos = 12
                    while pos + 8 <= len(blob_data):
                        chunk_id = blob_data[pos:pos+4]
                        chunk_len = struct.unpack("<I", blob_data[pos+4:pos+8])[0]
                        if chunk_id == b"fmt ":
                            fmt = blob_data[pos+8:pos+8+chunk_len]
                            audio_fmt, channels, samplerate, byterate, blockalign, bits = struct.unpack("<HHIIHH", fmt[:16])
                        elif chunk_id == b"data":
                            if samplerate > 0 and channels > 0 and bits > 0:
                                b_per_sec = samplerate * channels * (bits // 8)
                                if b_per_sec > 0:
                                    dur = chunk_len / b_per_sec
                                    duration_str = f"{dur:.2f}s"
                                    valid_wav = True
                        pos += 8 + chunk_len
                except Exception:
                    valid_wav = False
            
            if valid_wav and ef_count == 0:
                valid_wavs += 1
            else:
                corrupted_wavs += 1
                print(f"  ❌ {fname}: Size={len(blob_data)}, RIFF={is_riff}, EF_BF_BD={ef_count}, SR={samplerate}, Ch={channels}, Bits={bits}, Dur={duration_str}")
        
        print(f"Result for {short_hash}: Valid={valid_wavs}, Corrupted={corrupted_wavs}")

if __name__ == "__main__":
    audit_git_history()
