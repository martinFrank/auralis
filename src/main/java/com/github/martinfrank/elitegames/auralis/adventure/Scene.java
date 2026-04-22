package com.github.martinfrank.elitegames.auralis.adventure;

public record Scene (String title, String common, String details, String heraldInfo){

    public String fullContent(){

        String result = "";
        if (common != null){
            result = result + AdventureParser.COMMON_INFO_TITLE+"\n"+ common +"\n\n";
        }
        if (details != null){
            result = result + AdventureParser.DETAILS_INFO_TITLE+"\n"+details+"\n\n";
        }
        if (heraldInfo != null){
            result = result + AdventureParser.HERALD_INFO_TITLE+"\n"+heraldInfo+"\n\n";
        }
        return result;
    }
}
