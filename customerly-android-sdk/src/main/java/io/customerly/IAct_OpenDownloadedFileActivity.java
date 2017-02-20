package io.customerly;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class IAct_OpenDownloadedFileActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent input = this.getIntent();
        if(input != null) {
            Uri targetUri = input.getData();
            if(targetUri != null) {
                this.startActivity(new Intent(Intent.ACTION_VIEW, targetUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            }
        }
        this.finish();
    }
}
