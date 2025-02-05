package com.example.chasetags.runes;

import androidx.annotation.NonNull;

import com.example.chasetags.utils.GlobalVariables;
import java.util.HashMap;
import java.util.UUID;


public class Rune {

    private String runeID = "";
    private String ownerUID = "";
    private String type = "";
    private String name = "";
    private String value = "";
    private String loc = "";
    private String score = "0";
    private HashMap<String, Boolean> foundBy = new HashMap<>();
    private boolean checked;

    public Rune() {
    }

    public Rune(String type ,String ownerUID, String name, String value, String localisationClue, String score) {
        setRuneID(UUID.randomUUID().toString());
        setOwnerUID(ownerUID);
        setType(type);
        setName(name);
        setScore(score);
        setValue(value);
        setLocalisationClue(localisationClue);
        setChecked(false);
    }


    public void setRuneID(String s) { if (s != null) this.runeID = s; }

    public void setOwnerUID(String s) {
        this.ownerUID = s;
    }

    public void setType(String s) {
        this.type = s;
    }

    public void setName(String s) {
        this.name = s;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setLocalisationClue(String s) {
        this.loc = s;
    }

    public void setChecked(boolean b) {
        this.checked = b;
    }

    public void setScore(String score){
        this.score = score;
    }


    public String getRuneID() {
        return runeID;
    }

    public String getOwnerUID() {
        return ownerUID;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getLocalisationClue() {
        return loc;
    }

    public String getScore(){return score;}

    // Compare runes according to types and names sorted that way:
    // Code < Nfc < QR and in alphabetical order when they're the same type
    public int compareRunes(Rune rune2) {
        if (this.getType().equals(rune2.getType())) {
            return this.getName().compareToIgnoreCase(rune2.getName());
        } else if (this.getType().equals(GlobalVariables.Rune_code_object_type)) {
            return -1;
        } else if (this.getType().equals(GlobalVariables.Rune_nfc_object_type)) {
            if (rune2.getType().equals(GlobalVariables.Rune_code_object_type)) {
                return 1;
            } else {
                return -1;
            }
        } else {
            if (rune2.getType().equals(GlobalVariables.Rune_code_object_type)) {
                return 1;
            } else {
                return +1;
            }
        }
    }

    public boolean isChecked() {
        return checked;
    }

    public HashMap<String, Boolean> getFoundBy() {
        return foundBy;
    }

    public void addPlayerToFoundBy(String playerName) {
        foundBy.put(playerName, true);
    }

    @NonNull
    public String toString() { return "Name: "+name+" ID: "+runeID+" value: "+value+" hint: "+loc; }
}
