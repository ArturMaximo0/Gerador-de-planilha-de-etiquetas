package com.arturmaximo.etiquetas;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int CREATE_DOCUMENT_REQUEST = 1001;
    private static final String DOCX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final int INK = Color.rgb(36, 40, 32);
    private static final int MUTED = Color.rgb(102, 107, 96);
    private static final int GREEN = Color.rgb(69, 85, 56);
    private static final int PAPER = Color.rgb(245, 244, 239);

    private EditText textInput;
    private TextView status;
    private final List<TextView> previewLabels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(PAPER);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(createContent());
    }

    private View createContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(40), dp(24), dp(32));
        scroll.addView(page);

        TextView eyebrow = text("ETIQUETAS PARA IMPRIMIR", 12, GREEN, true);
        eyebrow.setLetterSpacing(0.1f);
        page.addView(eyebrow, bottomMargin(20));

        TextView title = text("Suas etiquetas,\ndo seu jeito.", 36, INK, false);
        title.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        page.addView(title);

        TextView intro = text(
                "Digite o nome do tempero ou qualquer outro texto. Gere o arquivo pronto para imprimir, direto no celular.",
                16, MUTED, false);
        intro.setLineSpacing(dp(4), 1f);
        page.addView(intro, bottomMargin(24));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(22), dp(20), dp(20));
        card.setBackground(roundRect(Color.WHITE, dp(16), Color.rgb(228, 227, 218)));
        page.addView(card);

        TextView label = text("Texto das etiquetas", 14, INK, true);
        card.addView(label, bottomMargin(8));

        textInput = new EditText(this);
        textInput.setSingleLine(true);
        textInput.setTextSize(17);
        textInput.setTextColor(INK);
        textInput.setHintTextColor(Color.rgb(140, 143, 133));
        textInput.setHint("Ex.: Alecrim");
        textInput.setPadding(dp(14), 0, dp(14), 0);
        textInput.setBackground(roundRect(Color.WHITE, dp(9), Color.rgb(213, 215, 202)));
        textInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        textInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                updatePreview(value.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable value) {}
        });
        card.addView(textInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        TextView hint = text(
                "O texto será repetido nas 120 etiquetas. Textos curtos tendem a imprimir melhor.",
                13, MUTED, false);
        hint.setLineSpacing(dp(3), 1f);
        card.addView(hint, topMargin(8));

        TextView previewTitle = text("PRÉVIA DA FOLHA · 6 COLUNAS × 20 LINHAS", 11, MUTED, true);
        previewTitle.setPadding(0, dp(22), 0, dp(10));
        card.addView(previewTitle);
        card.addView(createPreview());

        Button saveButton = button("Salvar arquivo .docx", true);
        card.addView(saveButton, topMargin(20));
        saveButton.setOnClickListener(view -> saveDocument());

        Button shareButton = button("Compartilhar arquivo", false);
        card.addView(shareButton, topMargin(10));
        shareButton.setOnClickListener(view -> shareDocument());

        status = text("", 14, MUTED, false);
        status.setGravity(Gravity.CENTER);
        card.addView(status, topMargin(12));

        TextView offlineNote = text("Funciona sem internet. O arquivo .docx pode ser enviado pelo WhatsApp.", 13, MUTED, false);
        offlineNote.setGravity(Gravity.CENTER);
        offlineNote.setPadding(dp(8), dp(20), dp(8), 0);
        page.addView(offlineNote);

        return scroll;
    }

    private View createPreview() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setBackground(roundRect(Color.WHITE, dp(8), Color.rgb(229, 228, 220)));

        for (int row = 0; row < 2; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            for (int column = 0; column < 6; column++) {
                TextView cell = text("ALECRIM", 9, GREEN, false);
                cell.setGravity(Gravity.CENTER);
                cell.setSingleLine(true);
                cell.setEllipsize(android.text.TextUtils.TruncateAt.END);
                cell.setPadding(dp(2), 0, dp(2), 0);
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                        0, dp(38), 1f);
                cellParams.rightMargin = dp(1);
                cellParams.bottomMargin = dp(1);
                cell.setBackgroundColor(PAPER);
                rowLayout.addView(cell, cellParams);
                previewLabels.add(cell);
            }
            grid.addView(rowLayout);
        }
        return grid;
    }

    private void saveDocument() {
        String value = getText();
        if (value == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(DOCX_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_TITLE, fileName(value));
        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST);
    }

    private void shareDocument() {
        String value = getText();
        if (value == null) {
            return;
        }

        File directory = new File(getCacheDir(), "shared");
        if (!directory.exists() && !directory.mkdirs()) {
            showError("Não foi possível preparar o arquivo para compartilhar.");
            return;
        }

        File document = new File(directory, fileName(value));
        try (OutputStream output = new FileOutputStream(document)) {
            writeDocument(output, value);
            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", document);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(DOCX_MIME_TYPE);
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "Etiquetas " + value);
            share.setClipData(android.content.ClipData.newUri(getContentResolver(), "Etiquetas", uri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Enviar arquivo .docx"));
            showSuccess("Arquivo pronto para compartilhar.");
        } catch (IOException error) {
            showError(error.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CREATE_DOCUMENT_REQUEST || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }

        String value = getText();
        if (value == null) {
            return;
        }
        try (OutputStream output = getContentResolver().openOutputStream(data.getData())) {
            if (output == null) {
                throw new IOException("Não foi possível gravar no local escolhido.");
            }
            writeDocument(output, value);
            showSuccess("Arquivo .docx salvo.");
        } catch (IOException error) {
            showError(error.getMessage());
        }
    }

    private String getText() {
        String value = textInput.getText().toString().trim();
        if (value.isEmpty()) {
            textInput.setError("Digite o texto das etiquetas.");
            textInput.requestFocus();
            return null;
        }
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(textInput.getWindowToken(), 0);
        return value;
    }

    private void writeDocument(OutputStream output, String value) throws IOException {
        try (java.io.InputStream template = getAssets().open("6.docx")) {
            DocxGenerator.write(template, output, value);
        }
    }

    private void updatePreview(String value) {
        String preview = value.isEmpty() ? "ALECRIM" : value;
        int size = preview.length() > 14 ? 7 : preview.length() > 9 ? 8 : 9;
        for (TextView label : previewLabels) {
            label.setText(preview);
            label.setTextSize(size);
        }
    }

    private void showSuccess(String message) {
        status.setText(message);
        status.setTextColor(GREEN);
    }

    private void showError(String message) {
        status.setText(message == null ? "Não foi possível gerar o arquivo." : message);
        status.setTextColor(Color.rgb(161, 62, 49));
    }

    private String fileName(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String safe = normalized.replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-|-$", "").toLowerCase(java.util.Locale.ROOT);
        if (safe.length() > 40) {
            safe = safe.substring(0, 40).replaceAll("-$", "");
        }
        return "etiquetas-" + (safe.isEmpty() ? "personalizadas" : safe) + ".docx";
    }

    private Button button(String title, boolean primary) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : GREEN);
        button.setBackground(roundRect(
                primary ? GREEN : Color.rgb(239, 241, 234), dp(9),
                primary ? GREEN : Color.rgb(224, 227, 216)));
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return text;
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(radius);
        background.setStroke(dp(1), strokeColor);
        return background;
    }

    private LinearLayout.LayoutParams bottomMargin(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams topMargin(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
