package com.audacious_software.question_kit.cards;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.audacious_software.question_kit.QuestionsActivity;
import com.audacious_software.question_kit.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import androidx.appcompat.widget.LinearLayoutCompat;

public class SelectMultipleCard extends QuestionCard {
    private ArrayList<String> mSelected = new ArrayList<>();
    private LinearLayoutCompat mCheckBoxes = null;

    public SelectMultipleCard(QuestionsActivity activity, JSONObject prompt, String defaultLanguage) {
        super(activity, prompt, defaultLanguage);
    }

    protected void initializeView(JSONObject prompt, ViewGroup parent) throws JSONException {
        TextView promptLabel = parent.findViewById(R.id.prompt_label);

        promptLabel.setText(this.getLocalizedValue(prompt, "prompt"));

        this.mCheckBoxes = this.findViewById(R.id.checkbox_group);

        final SelectMultipleCard me = this;

        if (prompt.has("options")) {
            final JSONArray options = prompt.getJSONArray("options");

            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);

                final String value = option.getString("value");

                CheckBox checkBox = new CheckBox(this.getActivity());
                checkBox.setText(this.getLocalizedValue(option, "label"));

                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        me.mSelected.remove(value);

                        if (checked) {
                            me.mSelected.add(value);
                        }

                        me.updateValue(me.key(), me.mSelected);
                    }
                });

                this.mCheckBoxes.addView(checkBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
    }

    public String description() {
        try {
            return this.getLocalizedValue(this.getPrompt(), "prompt");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return super.description();
    }

    protected int getCardLayoutResource() {
        return R.layout.card_question_select_multiple;
    }

    public void updateValue(Object value) {
        ArrayList<String> selected = (ArrayList<String>) value;

        try {
            final JSONArray options = this.mPrompt.getJSONArray("options");

            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);

                CheckBox checkBox = (CheckBox) this.mCheckBoxes.getChildAt(i);

                String optionValue = option.getString("value");

                if (selected.contains(optionValue)) {
                    checkBox.setChecked(true);
                } else {
                    checkBox.setChecked(false);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.updateValue(this.key(), selected);
    }
}
