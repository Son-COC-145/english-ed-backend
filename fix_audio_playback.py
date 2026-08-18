import os

# 1. Update PronunciationPractice.tsx
path1 = r'c:\Coding\DoAn\english-app-frontend\src\pages\student\ipa\PronunciationPractice.tsx'
with open(path1, 'r', encoding='utf-8') as f:
    content1 = f.read()

target1 = '''        {word.audioUrl && (
          <button 
            onClick={() => wordAudioRef.current?.play()}
            className="group flex items-center gap-2 text-slate-600 hover:text-blue-600 bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-200 px-5 py-2.5 rounded-2xl font-bold transition-all w-full justify-center md:justify-start"
          >
            <span className="text-2xl group-hover:scale-110 transition-transform">🔊</span> 
            Nghe mẫu
            <audio ref={wordAudioRef} src={word.audioUrl} />
          </button>
        )}'''

replacement1 = '''        {word.audioUrl && (
          <button 
            type="button"
            onClick={() => {
              const audio = new Audio(word.audioUrl);
              audio.play().catch(err => console.error("Lỗi phát audio:", err));
            }}
            className="group flex items-center gap-2 text-slate-600 hover:text-blue-600 bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-200 px-5 py-2.5 rounded-2xl font-bold transition-all w-full justify-center md:justify-start cursor-pointer active:scale-95"
          >
            <span className="text-2xl group-hover:scale-110 transition-transform">🔊</span> 
            Nghe mẫu
          </button>
        )}'''

content1 = content1.replace(target1, replacement1)
with open(path1, 'w', encoding='utf-8') as f:
    f.write(content1)

# 2. Update IpaDetailScreen.tsx
path2 = r'c:\Coding\DoAn\english-app-frontend\src\pages\student\ipa\IpaDetailScreen.tsx'
with open(path2, 'r', encoding='utf-8') as f:
    content2 = f.read()

target2 = '''          <div className="flex gap-4">
            {phoneme.audioMaleUrl && (
              <button 
                onClick={() => maleAudioRef.current?.play()}
                className="flex items-center gap-2 bg-blue-50 text-blue-700 px-4 py-2 rounded-xl hover:bg-blue-100 font-medium transition-colors"
              >
                <span>👨</span> Giọng Nam
                <audio ref={maleAudioRef} src={phoneme.audioMaleUrl} />
              </button>
            )}
            {phoneme.audioFemaleUrl && (
              <button 
                onClick={() => femaleAudioRef.current?.play()}
                className="flex items-center gap-2 bg-pink-50 text-pink-700 px-4 py-2 rounded-xl hover:bg-pink-100 font-medium transition-colors"
              >
                <span>👩</span> Giọng Nữ
                <audio ref={femaleAudioRef} src={phoneme.audioFemaleUrl} />
              </button>
            )}
          </div>'''

replacement2 = '''          <div className="flex gap-4">
            {phoneme.audioMaleUrl && (
              <button 
                type="button"
                onClick={() => {
                  const audio = new Audio(phoneme.audioMaleUrl);
                  audio.play().catch(e => console.error("Lỗi phát audio nam:", e));
                }}
                className="flex items-center gap-2 bg-blue-50 text-blue-700 px-4 py-2 rounded-xl hover:bg-blue-100 font-medium transition-colors cursor-pointer active:scale-95 shadow-sm"
              >
                <span>👨</span> Giọng Nam
              </button>
            )}
            {phoneme.audioFemaleUrl && (
              <button 
                type="button"
                onClick={() => {
                  const audio = new Audio(phoneme.audioFemaleUrl);
                  audio.play().catch(e => console.error("Lỗi phát audio nữ:", e));
                }}
                className="flex items-center gap-2 bg-pink-50 text-pink-700 px-4 py-2 rounded-xl hover:bg-pink-100 font-medium transition-colors cursor-pointer active:scale-95 shadow-sm"
              >
                <span>👩</span> Giọng Nữ
              </button>
            )}
          </div>'''

content2 = content2.replace(target2, replacement2)
with open(path2, 'w', encoding='utf-8') as f:
    f.write(content2)

print('Updated both files with new Audio() playback mechanism.')
