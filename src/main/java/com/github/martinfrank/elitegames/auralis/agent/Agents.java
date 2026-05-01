package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.agent.chat.*;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class Agents {

    private SetupQuestAgent setupQuestAgent;
    private ClassifyInputAgent classifyInputAgent;
    private QuestionResponseAgent questionResponseAgent;
    private AmbientResponseAgent ambientResponseAgent;
    private OocResponseAgent oocResponseAgent;
    private UnclearResponseAgent unclearResponseAgent;
    private DialogResponseAgent dialogResponseAgent;
    private ActionJudgeAgent actionJudgeAgent;
    private ActionResponseAgent actionResponseAgent;

    public static Agents allWithDefaults(ChatLanguageModel model) {
        Agents a = new Agents();
        a.setSetupQuestAgent(new SetupQuestAgent(model));
        a.setClassifyInputAgent(new ClassifyInputAgent(model));
        a.setQuestionResponseAgent(new QuestionResponseAgent(model));
        a.setAmbientResponseAgent(new AmbientResponseAgent(model));
        a.setOocResponseAgent(new OocResponseAgent(model));
        a.setUnclearResponseAgent(new UnclearResponseAgent(model));
        a.setDialogResponseAgent(new DialogResponseAgent(model));
        a.setActionJudgeAgent(new ActionJudgeAgent(model));
        a.setActionResponseAgent(new ActionResponseAgent(model));
        return a;
    }

    public SetupQuestAgent getSetupQuestAgent() { return setupQuestAgent; }
    public void setSetupQuestAgent(SetupQuestAgent v) { this.setupQuestAgent = v; }

    public ClassifyInputAgent getClassifyInputAgent() { return classifyInputAgent; }
    public void setClassifyInputAgent(ClassifyInputAgent v) { this.classifyInputAgent = v; }

    public QuestionResponseAgent getQuestionResponseAgent() { return questionResponseAgent; }
    public void setQuestionResponseAgent(QuestionResponseAgent v) { this.questionResponseAgent = v; }

    public AmbientResponseAgent getAmbientResponseAgent() { return ambientResponseAgent; }
    public void setAmbientResponseAgent(AmbientResponseAgent v) { this.ambientResponseAgent = v; }

    public OocResponseAgent getOocResponseAgent() { return oocResponseAgent; }
    public void setOocResponseAgent(OocResponseAgent v) { this.oocResponseAgent = v; }

    public UnclearResponseAgent getUnclearResponseAgent() { return unclearResponseAgent; }
    public void setUnclearResponseAgent(UnclearResponseAgent v) { this.unclearResponseAgent = v; }

    public DialogResponseAgent getDialogResponseAgent() { return dialogResponseAgent; }
    public void setDialogResponseAgent(DialogResponseAgent v) { this.dialogResponseAgent = v; }

    public ActionJudgeAgent getActionJudgeAgent() { return actionJudgeAgent; }
    public void setActionJudgeAgent(ActionJudgeAgent v) { this.actionJudgeAgent = v; }

    public ActionResponseAgent getActionResponseAgent() { return actionResponseAgent; }
    public void setActionResponseAgent(ActionResponseAgent v) { this.actionResponseAgent = v; }
}
