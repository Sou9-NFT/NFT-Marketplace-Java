package org.esprit.utils;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import java.util.ArrayList;
import java.util.List;

public class TextToSpeech {
    private static TextToSpeech instance;
    private Voice voice;
    private boolean speaking = false;
    private static final String[] VOICE_NAMES = {"kevin", "kevin16", "alan", "alan16"};

    private TextToSpeech() {
        System.setProperty("freetts.voices", 
            "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory," +
            "com.sun.speech.freetts.en.us.cmu_time_awb.AlanVoiceDirectory");
        setVoice("kevin16"); // Default voice
    }

    public static TextToSpeech getInstance() {
        if (instance == null) {
            instance = new TextToSpeech();
        }
        return instance;
    }

    public void setVoice(String voiceName) {
        Voice newVoice = VoiceManager.getInstance().getVoice(voiceName);
        if (newVoice != null) {
            if (voice != null) {
                voice.deallocate();
            }
            voice = newVoice;
            voice.allocate();
        } else {
            System.err.println("Could not find voice: " + voiceName);
        }
    }

    public List<String> getAvailableVoices() {
        List<String> availableVoices = new ArrayList<>();
        VoiceManager voiceManager = VoiceManager.getInstance();
        for (String voiceName : VOICE_NAMES) {
            Voice v = voiceManager.getVoice(voiceName);
            if (v != null) {
                availableVoices.add(voiceName);
            }
        }
        return availableVoices;
    }

    public void speak(String text) {
        if (voice == null) {
            System.err.println("Error: Could not initialize text-to-speech voice");
            return;
        }

        // Stop any current speech
        if (speaking) {
            stop();
        }

        // Speak in a separate thread to not block the UI
        new Thread(() -> {
            speaking = true;
            voice.speak(text);
            speaking = false;
        }).start();
    }

    public void stop() {
        if (voice != null) {
            voice.deallocate();
            voice.allocate();
            speaking = false;
        }
    }

    public boolean isSpeaking() {
        return speaking;
    }
}
