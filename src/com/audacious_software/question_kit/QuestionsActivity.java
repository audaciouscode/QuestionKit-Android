package com.audacious_software.question_kit;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import com.audacious_software.question_kit.cards.DateRangeCard;
import com.audacious_software.question_kit.cards.ReadOnlyLocationCard;
import com.audacious_software.question_kit.cards.MultiLineTextInputCard;
import com.audacious_software.question_kit.cards.QuestionCard;
import com.audacious_software.question_kit.cards.ReadOnlyTextCard;
import com.audacious_software.question_kit.cards.SelectMultipleCard;
import com.audacious_software.question_kit.cards.SelectOneCard;
import com.audacious_software.question_kit.cards.SelectTimeCard;
import com.audacious_software.question_kit.cards.SingleLineTextInputCard;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

public class QuestionsActivity extends AppCompatActivity {
    private FloatingActionButton mCompleteButton;

    public interface QuestionsUpdatedListener {
        void onQuestionsUpdated(Map<String, Object> answers, boolean isComplete);
        void onCompleted(String questions, Map<String, Object> answers);
    }

    public static final String JSON_DEFINITION = "com.audacious_software.question_kit.QuestionsActivity.JSON_DEFINITION";
    private LinearLayout mRootLayout = null;
    private ArrayList<QuestionCard> mQuestionCards = new ArrayList<>();
    private Runnable mUpdateRunnable = null;
    private Handler mHandler = null;

    private HashMap<String, Object> mAnswers;
    private JSONObject mDefinition;

    private ArrayList <QuestionsUpdatedListener> mQuestionListeners = new ArrayList<>();

    public int layoutResource() {
        return R.layout.activity_question;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(this.layoutResource());

        ActionBar actionbar = this.getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);

        final QuestionsActivity me = this;

        final String jsonDefinition = this.getQuestions();

        this.mRootLayout = this.findViewById(R.id.questions_root);

        this.mCompleteButton = this.findViewById(R.id.complete_button);

        this.mCompleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (QuestionsUpdatedListener listener : me.mQuestionListeners) {
                    listener.onCompleted(jsonDefinition, me.mAnswers);
                }
            }
        });

        this.mCompleteButton.hide();

        this.mAnswers = new HashMap<>();

        this.mUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                me.refreshCards();
            }
        };

        this.mHandler = new Handler(Looper.getMainLooper());

        try {
            this.updateQuestions(new JSONObject(jsonDefinition));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected String getQuestions() {
        return this.getIntent().getStringExtra(QuestionsActivity.JSON_DEFINITION);
    }

    private void refreshCards() {
        List<String> activePrompts = this.fetchActivePrompts();

        for (QuestionCard card : this.mQuestionCards) {
            String key = card.key();

            if (activePrompts.contains(key)) {
                card.setVisibility(View.VISIBLE);
            } else {
                card.setVisibility(View.GONE);
            }
        }

        boolean isComplete = this.questionsComplete();

        for (QuestionsUpdatedListener listener : this.mQuestionListeners) {
            listener.onQuestionsUpdated(this.mAnswers, isComplete);
        }
    }

    private void updateQuestions(JSONObject definition) throws JSONException {
        this.mDefinition = definition;

        if (this.mDefinition.has("name")) {
            this.updateActivityTitle(this.mDefinition.getString("name"));
        }

        this.mRootLayout.removeAllViews();
        this.mQuestionCards.clear();

        JSONArray prompts = this.mDefinition.getJSONArray("prompts");

        for (int i = 0; i < prompts.length(); i++) {
            QuestionCard card = this.cardForPrompt(prompts.getJSONObject(i));

            if (i == 0) {
                card.setIsFirstCard(true);
            } else {
                card.setIsFirstCard(false);
            }

            this.mRootLayout.addView(card);

            this.mQuestionCards.add(card);
        }

        this.refreshCards();

        this.mRootLayout.invalidate();
    }

    public QuestionCard cardForPrompt(JSONObject prompt) throws JSONException {
        if ("multi-line".equals(prompt.getString("prompt-type"))) {
            return new MultiLineTextInputCard(this, prompt);
        } else if ("date-range".equals(prompt.getString("prompt-type"))) {
            return new DateRangeCard(this, prompt);
        } else if ("single-line".equals(prompt.getString("prompt-type"))) {
            return new SingleLineTextInputCard(this, prompt);
        } else if ("select-one".equals(prompt.getString("prompt-type"))) {
            return new SelectOneCard(this, prompt);
        } else if ("select-multiple".equals(prompt.getString("prompt-type"))) {
            return new SelectMultipleCard(this, prompt);
        } else if ("select-time".equals(prompt.getString("prompt-type"))) {
            return new SelectTimeCard(this, prompt);
        } else if ("read-only-text".equals(prompt.getString("prompt-type"))) {
            return new ReadOnlyTextCard(this, prompt);
        } else if ("read-only-location".equals(prompt.getString("prompt-type"))) {
            return new ReadOnlyLocationCard(this, prompt);
        }

        return new QuestionCard(this, prompt);
    }

    private void updateActivityTitle(String title) {
        this.getSupportActionBar().setTitle(title);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DateRangeActivity.SELECT_DATE_RANGE) {
            String key = data.getStringExtra(DateRangeActivity.QUESTION_KEY);

            if (key == null) {
                key = "";
            }

            for (QuestionCard card : this.mQuestionCards) {
                if (key.equals(card.key())) {
                    DateRangeCard dateCard = (DateRangeCard) card;

                    long start = data.getLongExtra(DateRangeActivity.RANGE_START, 0);
                    long end = data.getLongExtra(DateRangeActivity.RANGE_END, 0);

                    if (start != 0 && end != 0) {
                        dateCard.setSelectedRange(start, end);
                    }
                }
            }
        }
    }

    public void updateValue(String key, Object value) {
        if (value != null) {
            this.mAnswers.put(key, value);
        } else {
            this.mAnswers.remove(key);
        }

        this.mHandler.removeCallbacks(this.mUpdateRunnable);

        this.mHandler.postDelayed(this.mUpdateRunnable, 1000);

        boolean isComplete = this.questionsComplete();

        for (QuestionsUpdatedListener listener : this.mQuestionListeners) {
            listener.onQuestionsUpdated(this.mAnswers, isComplete);
        }
    }

    protected String descriptionForKey(String key) {
        for (QuestionCard card : this.mQuestionCards) {
            if (card.key().equals(key)) {
                return card.description();
            }
        }

        return key;
    }

    public List<String> incompleteQuestions() {
        ArrayList<String> incomplete = new ArrayList<>();

        try {
            JSONArray completedActions = this.mDefinition.getJSONArray("completed-actions");

            for (int i = 0; i < completedActions.length(); i++) {
                JSONObject action = completedActions.getJSONObject(i);

                JSONArray constraints = action.getJSONArray("constraints");

                List<JSONObject> pendingConstraints = this.pendingConstraints(constraints, this.mAnswers);

                if (pendingConstraints.size() == 0) {
                    JSONArray actions = action.getJSONArray("actions");

                    for (int j = 0; j < actions.length(); j++) {
                        try {
                            String actionName = actions.getJSONObject(i).getString("action");

                            if ("complete".equals(actionName)) {
                                return new ArrayList<>();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    for (JSONObject constraint : pendingConstraints) {
                        String key = constraint.getString("key");

                        if (incomplete.contains(key) == false) {
                            incomplete.add(key);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return incomplete;
    }

    private boolean questionsComplete() {
        return this.incompleteQuestions().size() == 0;
    }

    private boolean evaluateConstraints(JSONArray constraints, HashMap<String,Object> answers) throws JSONException {
        return this.pendingConstraints(constraints, answers).size() == 0;
    }

    private List<JSONObject> pendingConstraints(JSONArray constraints, HashMap<String,Object> answers) throws JSONException {
        List<JSONObject> pending = new ArrayList<>();

        for (int j = 0; j < constraints.length(); j++) {
            JSONObject constraint = constraints.getJSONObject(j);

            String key = constraint.getString("key");
            String operator = constraint.getString("operator");
            Object value = constraint.get("value");

            Object answer = this.mAnswers.get(key);

            if (answer == null) {
                if (pending.contains(constraint) == false) {
                    pending.add(constraint);
                }
            } else {
                if ("=".equals(operator) && answer.equals(value) == false) {
                    if (pending.contains(constraint) == false) {
                        pending.add(constraint);
                    }
                } else if ("!=".equals(operator) && answer.equals(value)) {
                    if (pending.contains(constraint) == false) {
                        pending.add(constraint);
                    }
                } else if ("<".equals(operator)) {
                    if (value instanceof Comparable && answer instanceof Comparable) {
                        Comparable valueComparable = (Comparable) value;
                        Comparable answerComparable = (Comparable) answer;

                        //noinspection unchecked
                        if (answerComparable.compareTo(valueComparable) > -1) {
                            if (pending.contains(constraint) == false) {
                                pending.add(constraint);
                            }
                        }
                    }
                } else if ("<=".equals(operator)) {
                    if (value instanceof Comparable && answer instanceof Comparable) {
                        Comparable valueComparable = (Comparable) value;
                        Comparable answerComparable = (Comparable) answer;

                        //noinspection unchecked
                        if (answerComparable.compareTo(valueComparable) > 0) {
                            if (pending.contains(constraint) == false) {
                                pending.add(constraint);
                            }
                        }
                    }
                } else if (">".equals(operator)) {
                    if (value instanceof Comparable && answer instanceof Comparable) {
                        Comparable valueComparable = (Comparable) value;
                        Comparable answerComparable = (Comparable) answer;

                        //noinspection unchecked
                        if (answerComparable.compareTo(valueComparable) < 1) {
                            if (pending.contains(constraint) == false) {
                                pending.add(constraint);
                            }
                        }
                    }
                } else if (">=".equals(operator)) {
                    if (value instanceof Comparable && answer instanceof Comparable) {
                        Comparable valueComparable = (Comparable) value;
                        Comparable answerComparable = (Comparable) answer;

                        //noinspection unchecked
                        if (answerComparable.compareTo(valueComparable) < 0) {
                            if (pending.contains(constraint) == false) {
                                pending.add(constraint);
                            }
                        }
                    }
                } else if ("in".equals(operator)) {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> selected = (ArrayList<String>) answer;

                    if (selected.contains(value) == false) {
                        if (pending.contains(constraint) == false) {
                            pending.add(constraint);
                        }
                    }
                }
            }
        }

        return pending;
    }

    private boolean evaluateOrConstraints(JSONArray constraints, HashMap<String,Object> answers) throws JSONException {
        boolean passes = false;

        for (int j = 0; j < constraints.length(); j++) {
            JSONObject constraint = constraints.getJSONObject(j);

            String key = constraint.getString("key");
            String operator = constraint.getString("operator");
            Object value = constraint.get("value");

            Object answer = this.mAnswers.get(key);

            if (answer != null) {
                if ("=".equals(operator) && answer.equals(value)) {
                    passes = true;
                } else if ("!=".equals(operator) && answer.equals(value) == false) {
                    passes = true;
                } else if ("<".equals(operator)) {
                    if (value instanceof Comparable && answer instanceof Comparable) {
                        Comparable valueComparable = (Comparable) value;
                        Comparable answerComparable = (Comparable) answer;

                        //noinspection unchecked
                        if (answerComparable.compareTo(valueComparable) < 1) {
                            passes = true;
                        }
                    }
                } else if ("<=".equals(operator)) {
                    if (value instanceof Comparable && answer instanceof Comparable) {
                        Comparable valueComparable = (Comparable) value;
                        Comparable answerComparable = (Comparable) answer;

                        //noinspection unchecked
                        if (answerComparable.compareTo(valueComparable) <= 0) {
                            passes = true;
                        }
                    }
                } else if (">".equals(operator)) {
                    if (value instanceof Comparable && answer instanceof Comparable) {
                        Comparable valueComparable = (Comparable) value;
                        Comparable answerComparable = (Comparable) answer;

                        //noinspection unchecked
                        if (answerComparable.compareTo(valueComparable) > -1) {
                            passes = true;
                        }
                    }
                } else if (">=".equals(operator)) {
                    if (value instanceof Comparable && answer instanceof Comparable) {
                        Comparable valueComparable = (Comparable) value;
                        Comparable answerComparable = (Comparable) answer;

                        //noinspection unchecked
                        if (answerComparable.compareTo(valueComparable) >= 0) {
                            passes = true;
                        }
                    }
                } else if ("in".equals(operator)) {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> selected = (ArrayList<String>) answer;

                    if (selected.contains(value)) {
                        passes = true;
                    }
                }
            }
        }

        return passes;
    }

    private List<String> fetchActivePrompts() {
        ArrayList<String> active = new ArrayList<>();

        try {
            JSONArray prompts = this.mDefinition.getJSONArray("prompts");

            for (int i = 0; i < prompts.length(); i++) {
                JSONObject prompt = prompts.getJSONObject(i);

                if (prompt.has("constraints")) {
                    JSONArray constraints = prompt.getJSONArray("constraints");

                    if (prompt.has("constraint-matches") && "any".equals(prompt.getString("constraint-matches"))) {
                        if (this.evaluateOrConstraints(constraints, this.mAnswers)) {
                            active.add(prompt.getString("key"));
                        }
                    } else {
                        if (this.evaluateConstraints(constraints, this.mAnswers)) {
                            active.add(prompt.getString("key"));
                        }
                    }
                } else {
                    active.add(prompt.getString("key"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return active;
    }

    protected void addQuestionsUpdatedListener(QuestionsUpdatedListener listener) {
        if (this.mQuestionListeners.contains(listener) == false) {
            this.mQuestionListeners.add(listener);
        }
    }

    protected void removeQuestionsUpdatedListener(QuestionsUpdatedListener listener) {
        this.mQuestionListeners.remove(listener);
    }

    protected void displayCompleteButton() {
        this.mCompleteButton.show();
    }

    protected void hideCompleteButton() {
        this.mCompleteButton.hide();
    }

    protected void setCompleteButtonIcon(int imageResource) {
        this.mCompleteButton.setImageDrawable(ContextCompat.getDrawable(this, imageResource));
    }

    public JSONObject addLocalizedText(JSONObject existingObject, String code, String text) {
        if (existingObject == null) {
            existingObject = new JSONObject();
        }

        if (code == null) {
            LocaleListCompat locales = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration());

            code = locales.get(0).getLanguage();
        }

        try {
            existingObject.put(code, text);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return existingObject;
    }
}
