#!/usr/bin/env python3
"""
Script to generate stroke order JSON files for Hangul characters
Based on standard Hangul stroke order rules
"""

import json
import os
from pathlib import Path

# Mapping từ chữ cái sang tên file
CHAR_TO_FILENAME = {
    "ㅏ": "a_stroke.json",
    "ㅑ": "ya_stroke.json",
    "ㅓ": "eo_stroke.json",
    "ㅕ": "yeo_stroke.json",
    "ㅗ": "o_stroke.json",
    "ㅛ": "yo_stroke.json",
    "ㅜ": "u_stroke.json",
    "ㅠ": "yu_stroke.json",
    "ㅡ": "eu_stroke.json",
    "ㅣ": "i_stroke.json",
    "ㅐ": "ae_stroke.json",
    "ㅒ": "yae_stroke.json",
    "ㅔ": "e_stroke.json",
    "ㅖ": "ye_stroke.json",
    "ㅘ": "wa_stroke.json",
    "ㅙ": "wae_stroke.json",
    "ㅚ": "oe_stroke.json",
    "ㅝ": "wo_stroke.json",
    "ㅞ": "we_stroke.json",
    "ㅟ": "wi_stroke.json",
    "ㅢ": "ui_stroke.json",
    "ㄱ": "giyeok_stroke.json",
    "ㄴ": "nieun_stroke.json",
    "ㄷ": "digeut_stroke.json",
    "ㄹ": "rieul_stroke.json",
    "ㅁ": "mieum_stroke.json",
    "ㅂ": "bieup_stroke.json",
    "ㅅ": "siot_stroke.json",
    "ㅇ": "ieung_stroke.json",
    "ㅈ": "jieut_stroke.json",
    "ㅎ": "hieut_stroke.json",
    "ㅊ": "chieut_stroke.json",
    "ㅋ": "kieuk_stroke.json",
    "ㅌ": "tieut_stroke.json",
    "ㅍ": "pieup_stroke.json",
    "ㄲ": "ssanggiyeok_stroke.json",
    "ㄸ": "ssangdigeut_stroke.json",
    "ㅃ": "ssangbieup_stroke.json",
    "ㅆ": "ssangsiot_stroke.json",
    "ㅉ": "ssangjieut_stroke.json",
    "가": "ga_stroke.json",
}

# Stroke patterns cho mỗi chữ cái
# Coordinates normalized (0.0 - 1.0)
STROKE_PATTERNS = {
    # Nguyên âm cơ bản
    "ㅏ": [
        # Nét 1: Nét dọc
        [(0.5, 0.2), (0.5, 0.8)],
        # Nét 2: Nét ngang bên phải
        [(0.5, 0.5), (0.85, 0.5)]
    ],
    "ㅑ": [
        # Nét 1: Nét dọc
        [(0.5, 0.2), (0.5, 0.8)],
        # Nét 2: Nét ngang trên
        [(0.5, 0.4), (0.85, 0.4)],
        # Nét 3: Nét ngang dưới
        [(0.5, 0.6), (0.85, 0.6)]
    ],
    "ㅓ": [
        # Nét 1: Nét dọc
        [(0.5, 0.2), (0.5, 0.8)],
        # Nét 2: Nét ngang bên trái
        [(0.15, 0.5), (0.5, 0.5)]
    ],
    "ㅕ": [
        # Nét 1: Nét dọc
        [(0.5, 0.2), (0.5, 0.8)],
        # Nét 2: Nét ngang trên
        [(0.15, 0.4), (0.5, 0.4)],
        # Nét 3: Nét ngang dưới
        [(0.15, 0.6), (0.5, 0.6)]
    ],
    "ㅗ": [
        # Nét 1: Nét ngang
        [(0.2, 0.35), (0.8, 0.35)],
        # Nét 2: Nét dọc xuống
        [(0.5, 0.35), (0.5, 0.8)]
    ],
    "ㅛ": [
        # Nét 1: Nét ngang
        [(0.2, 0.3), (0.8, 0.3)],
        # Nét 2: Nét dọc xuống
        [(0.5, 0.3), (0.5, 0.8)],
        # Nét 3: Nét ngang thứ 2
        [(0.2, 0.5), (0.8, 0.5)]
    ],
    "ㅜ": [
        # Nét 1: Nét ngang ở dưới (từ trái sang phải)
        [(0.2, 0.65), (0.8, 0.65)],
        # Nét 2: Nét dọc từ dưới lên trên (từ điểm giữa nét ngang lên trên)
        [(0.5, 0.65), (0.5, 0.2)]
    ],
    "ㅠ": [
        # Nét 1: Nét ngang trên (từ trái sang phải)
        [(0.2, 0.45), (0.8, 0.45)],
        # Nét 2: Nét ngang dưới (từ trái sang phải)
        [(0.2, 0.7), (0.8, 0.7)],
        # Nét 3: Nét dọc từ dưới lên trên (từ điểm giữa nét ngang dưới lên trên)
        [(0.5, 0.7), (0.5, 0.2)]
    ],
    "ㅡ": [
        # Nét 1: Nét ngang
        [(0.2, 0.5), (0.8, 0.5)]
    ],
    "ㅣ": [
        # Nét 1: Nét dọc
        [(0.5, 0.2), (0.5, 0.8)]
    ],
    
    # Nguyên âm đôi
    "ㅐ": [
        # Nét 1: Nét dọc
        [(0.4, 0.2), (0.4, 0.8)],
        # Nét 2: Nét ngang bên phải trên
        [(0.4, 0.4), (0.75, 0.4)],
        # Nét 3: Nét ngang bên phải dưới
        [(0.4, 0.6), (0.75, 0.6)]
    ],
    "ㅒ": [
        # Nét 1: Nét dọc
        [(0.4, 0.2), (0.4, 0.8)],
        # Nét 2-5: Hai nét trên và dưới (mỗi bên có 2 nét)
        [(0.4, 0.35), (0.7, 0.35)],
        [(0.4, 0.45), (0.7, 0.45)],
        [(0.4, 0.55), (0.7, 0.55)],
        [(0.4, 0.65), (0.7, 0.65)]
    ],
    "ㅔ": [
        # Nét 1: Nét dọc
        [(0.6, 0.2), (0.6, 0.8)],
        # Nét 2: Nét ngang bên trái trên
        [(0.25, 0.4), (0.6, 0.4)],
        # Nét 3: Nét ngang bên trái dưới
        [(0.25, 0.6), (0.6, 0.6)]
    ],
    "ㅖ": [
        # Nét 1: Nét dọc
        [(0.6, 0.2), (0.6, 0.8)],
        # Nét 2-5: Hai nét trên và dưới (mỗi bên có 2 nét)
        [(0.3, 0.35), (0.6, 0.35)],
        [(0.3, 0.45), (0.6, 0.45)],
        [(0.3, 0.55), (0.6, 0.55)],
        [(0.3, 0.65), (0.6, 0.65)]
    ],
    "ㅘ": [
        # Kết hợp ㅗ và ㅏ
        [(0.3, 0.35), (0.7, 0.35)],  # Ngang của ㅗ
        [(0.5, 0.35), (0.5, 0.8)],   # Dọc xuống
        [(0.5, 0.5), (0.85, 0.5)]    # Ngang của ㅏ
    ],
    "ㅙ": [
        # Nét 1-2: ㅗ
        [(0.3, 0.35), (0.7, 0.35)],
        [(0.5, 0.35), (0.5, 0.8)],
        # Nét 3-4: ㅐ
        [(0.5, 0.45), (0.8, 0.45)],
        [(0.5, 0.65), (0.8, 0.65)]
    ],
    "ㅚ": [
        # Nét 1: Ngang
        [(0.2, 0.35), (0.8, 0.35)],
        # Nét 2: Dọc xuống
        [(0.5, 0.35), (0.5, 0.8)],
        # Nét 3: Ngang ngắn bên phải
        [(0.5, 0.5), (0.75, 0.5)]
    ],
    "ㅝ": [
        # Nét 1: Ngang ở dưới (từ trái sang phải) - phần ㅜ
        [(0.2, 0.65), (0.8, 0.65)],
        # Nét 2: Dọc từ dưới lên trên - phần ㅜ (từ điểm giữa nét ngang lên trên)
        [(0.5, 0.65), (0.5, 0.2)],
        # Nét 3: Ngang bên trái - phần ㅓ
        [(0.15, 0.5), (0.5, 0.5)]
    ],
    "ㅞ": [
        # Nét 1: Ngang ở dưới (từ trái sang phải) - phần ㅜ
        [(0.2, 0.65), (0.8, 0.65)],
        # Nét 2: Dọc từ dưới lên trên - phần ㅜ (từ điểm giữa nét ngang lên trên)
        [(0.5, 0.65), (0.5, 0.2)],
        # Nét 3-4: ㅔ
        [(0.25, 0.45), (0.6, 0.45)],
        [(0.25, 0.55), (0.6, 0.55)]
    ],
    "ㅟ": [
        # Nét 1: Ngang ở dưới (từ trái sang phải) - phần ㅜ
        [(0.2, 0.65), (0.8, 0.65)],
        # Nét 2: Dọc từ dưới lên trên - phần ㅜ (từ điểm giữa nét ngang lên trên)
        [(0.5, 0.65), (0.5, 0.2)],
        # Nét 3: Dọc từ trên xuống dưới bên phải - phần ㅣ (theo quy tắc: từ trên xuống)
        [(0.7, 0.4), (0.7, 0.65)]
    ],
    "ㅢ": [
        # Nét 1: Ngang
        [(0.2, 0.5), (0.8, 0.5)],
        # Nét 2: Dọc xuống từ giữa
        [(0.5, 0.5), (0.5, 0.8)]
    ],
    
    # Phụ âm cơ bản
    "ㄱ": [
        # Nét 1: Ngang trên, dọc xuống, ngang dưới (L-shape)
        [(0.2, 0.3), (0.6, 0.3), (0.6, 0.7), (0.2, 0.7)]
    ],
    "ㄴ": [
        # Nét 1: Dọc, ngang
        [(0.3, 0.25), (0.3, 0.75), (0.7, 0.75)]
    ],
    "ㄷ": [
        # Nét 1: Ngang trên, 2 dọc, ngang dưới
        [(0.25, 0.3), (0.75, 0.3), (0.75, 0.7), (0.25, 0.7)]
    ],
    "ㄹ": [
        # Nét 1: Dọc cong
        [(0.4, 0.25), (0.4, 0.5), (0.6, 0.5), (0.6, 0.75)]
    ],
    "ㅁ": [
        # Nét 1: Ngang trên
        [(0.3, 0.3), (0.7, 0.3)],
        # Nét 2: Dọc trái
        [(0.3, 0.3), (0.3, 0.7)],
        # Nét 3: Dọc phải
        [(0.7, 0.3), (0.7, 0.7)],
        # Nét 4: Ngang dưới
        [(0.3, 0.7), (0.7, 0.7)]
    ],
    "ㅂ": [
        # Nét 1: Ngang trên
        [(0.3, 0.3), (0.7, 0.3)],
        # Nét 2: Dọc giữa
        [(0.5, 0.3), (0.5, 0.7)],
        # Nét 3: Ngang dưới
        [(0.3, 0.7), (0.7, 0.7)]
    ],
    "ㅅ": [
        # Nét 1: Chéo từ trên xuống
        [(0.3, 0.25), (0.7, 0.75)],
        # Nét 2: Chéo từ dưới lên
        [(0.3, 0.75), (0.7, 0.25)]
    ],
    "ㅇ": [
        # Nét 1: Vòng tròn (oval) - simplified with key points
        [(0.4, 0.4), (0.5, 0.3), (0.6, 0.3), (0.7, 0.4), (0.7, 0.6), 
         (0.6, 0.7), (0.4, 0.7), (0.3, 0.6), (0.3, 0.4), (0.4, 0.4)]
    ],
    "ㅈ": [
        # Nét 1: Ngang trên
        [(0.3, 0.3), (0.7, 0.3)],
        # Nét 2: Dọc giữa
        [(0.5, 0.3), (0.5, 0.7)],
        # Nét 3: Ngang dưới ngắn
        [(0.4, 0.7), (0.6, 0.7)]
    ],
    "ㅎ": [
        # Nét 1: Ngang trên
        [(0.25, 0.3), (0.75, 0.3)],
        # Nét 2: Dọc giữa
        [(0.5, 0.3), (0.5, 0.7)],
        # Nét 3: Ngang giữa
        [(0.35, 0.5), (0.65, 0.5)],
        # Nét 4: Ngang dưới
        [(0.3, 0.7), (0.7, 0.7)]
    ],
    
    # Phụ âm bật hơi
    "ㅊ": [
        # Giống ㅈ nhưng có nét thêm ở trên
        [(0.3, 0.25), (0.7, 0.25)],  # Nét ngắn trên
        [(0.3, 0.35), (0.7, 0.35)],  # Ngang trên
        [(0.5, 0.35), (0.5, 0.7)],   # Dọc
        [(0.4, 0.7), (0.6, 0.7)]     # Ngang dưới
    ],
    "ㅋ": [
        # Giống ㄱ nhưng có nét dọc thêm
        [(0.2, 0.3), (0.6, 0.3), (0.6, 0.7), (0.2, 0.7)],
        [(0.7, 0.25), (0.7, 0.75)]  # Dọc phải
    ],
    "ㅌ": [
        # Giống ㄷ nhưng có nét dọc thêm
        [(0.25, 0.3), (0.75, 0.3), (0.75, 0.7), (0.25, 0.7)],
        [(0.8, 0.25), (0.8, 0.75)]  # Dọc phải
    ],
    "ㅍ": [
        # Giống ㅂ nhưng có nét ngang thêm
        [(0.3, 0.3), (0.7, 0.3)],
        [(0.5, 0.3), (0.5, 0.7)],
        [(0.3, 0.7), (0.7, 0.7)],
        [(0.25, 0.5), (0.75, 0.5)]  # Ngang giữa dài
    ],
    
    # Phụ âm căng (double)
    "ㄲ": [
        # Hai ㄱ
        [(0.15, 0.3), (0.45, 0.3), (0.45, 0.7), (0.15, 0.7)],
        [(0.55, 0.3), (0.85, 0.3), (0.85, 0.7), (0.55, 0.7)]
    ],
    "ㄸ": [
        # Hai ㄷ
        [(0.15, 0.3), (0.45, 0.3), (0.45, 0.7), (0.15, 0.7)],
        [(0.55, 0.3), (0.85, 0.3), (0.85, 0.7), (0.55, 0.7)]
    ],
    "ㅃ": [
        # Hai ㅂ
        [(0.2, 0.3), (0.45, 0.3)],
        [(0.325, 0.3), (0.325, 0.7)],
        [(0.2, 0.7), (0.45, 0.7)],
        [(0.55, 0.3), (0.8, 0.3)],
        [(0.675, 0.3), (0.675, 0.7)],
        [(0.55, 0.7), (0.8, 0.7)]
    ],
    "ㅆ": [
        # Hai ㅅ
        [(0.2, 0.25), (0.45, 0.75)],
        [(0.2, 0.75), (0.45, 0.25)],
        [(0.55, 0.25), (0.8, 0.75)],
        [(0.55, 0.75), (0.8, 0.25)]
    ],
    "ㅉ": [
        # Hai ㅈ
        [(0.2, 0.3), (0.45, 0.3)],
        [(0.325, 0.3), (0.325, 0.7)],
        [(0.25, 0.7), (0.4, 0.7)],
        [(0.55, 0.3), (0.8, 0.3)],
        [(0.675, 0.3), (0.675, 0.7)],
        [(0.6, 0.7), (0.75, 0.7)]
    ],
}


def generate_stroke_json(character: str, strokes: list) -> dict:
    """Generate JSON structure for a character"""
    stroke_list = []
    for idx, stroke_points in enumerate(strokes):
        points = [{"x": float(x), "y": float(y)} for x, y in stroke_points]
        stroke_list.append({
            "strokeIndex": idx,
            "points": points
        })
    
    return {
        "character": character,
        "strokes": stroke_list
    }


def main():
    # Output directory
    output_dir = Path("app/src/main/assets/strokes")
    output_dir.mkdir(parents=True, exist_ok=True)
    
    generated_count = 0
    skipped_count = 0
    
    # Generate JSON files for all characters
    for char, filename in CHAR_TO_FILENAME.items():
        if char in STROKE_PATTERNS:
            strokes = STROKE_PATTERNS[char]
            json_data = generate_stroke_json(char, strokes)
            
            output_path = output_dir / filename
            
            # Ghi đè file nếu đã tồn tại để cập nhật stroke patterns
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(json_data, f, ensure_ascii=False, indent=2)
            
            if output_path.exists() and output_path.stat().st_size > 0:
                # File đã tồn tại, đã được cập nhật
                print(f"🔄 Updated {filename} for {char} ({len(strokes)} strokes)")
                generated_count += 1
            else:
                print(f"✅ Generated {filename} for {char} ({len(strokes)} strokes)")
                generated_count += 1
        else:
            print(f"❌ No pattern defined for {char}")
    
    print(f"\n📊 Summary:")
    print(f"   Generated: {generated_count} files")
    print(f"   Skipped: {skipped_count} files")
    print(f"   Total: {generated_count + skipped_count} files")


if __name__ == "__main__":
    main()

