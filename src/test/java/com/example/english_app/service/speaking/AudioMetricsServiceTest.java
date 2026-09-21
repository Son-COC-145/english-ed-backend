package com.example.english_app.service.speaking;
import org.junit.jupiter.api.Test;
import javax.sound.sampled.*;
import java.io.*;
import static org.assertj.core.api.Assertions.*;

class AudioMetricsServiceTest {
    private final AudioMetricsService service=new AudioMetricsService();
    static byte[] wav(boolean pause) throws Exception {
        int rate=8000; byte[] samples=new byte[rate*2*2];
        for(int i=0;i<rate*2;i++) {
            double t=(double)i/rate;
            short value=(short)((pause && t>0.6 && t<1.1 ? 0 : Math.sin(2*Math.PI*200*t))*12000);
            samples[i*2]=(byte)value; samples[i*2+1]=(byte)(value>>8);
        }
        var output=new ByteArrayOutputStream();
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(samples),new AudioFormat(rate,16,1,true,false),rate*2),AudioFileFormat.Type.WAVE,output);
        return output.toByteArray();
    }
    @Test void measuresDurationPauseAndPitchFromAudio() throws Exception {
        var m=service.analyze(wav(true),"Um, I like coffee. Uh.",null);
        assertThat(m.get("durationSeconds")).isEqualTo(2.0);
        assertThat(m.get("pauseCount")).isEqualTo(1);
        assertThat(m.get("fillerCount")).isEqualTo(2);
        assertThat(m.get("wordCount")).isEqualTo(5);
        assertThat((java.util.List<?>)m.get("pitchContour")).isNotEmpty();
    }
    @Test void missingAudioDoesNotInventDurationOrScore() {
        var m=service.analyze(null,"I like coffee.",null);
        assertThat(m.get("fluencyStatus")).isEqualTo("INSUFFICIENT_DATA");
        assertThat(m).doesNotContainKeys("durationSeconds","wpm","pitchContour");
        assertThat(m.get("fillerCount")).isEqualTo(0);
    }
    @Test void ignoresClaimedMimeAndRejectsUnknownBytes() {
        assertThatThrownBy(()->AudioMetricsService.detectMime("this is not audio".getBytes()))
            .isInstanceOf(com.example.english_app.exception.AppException.class);
    }
    @Test void providerDurationDoesNotInventPauses() {
        var m=service.analyze(null,"hello there",2.0);
        assertThat(m.get("wpm")).isEqualTo(60.0);
        assertThat(m.get("pauseStatus")).isEqualTo("UNAVAILABLE");
    }
}
