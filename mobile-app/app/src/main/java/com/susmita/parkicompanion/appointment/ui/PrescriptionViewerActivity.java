package com.susmita.parkicompanion.appointment.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.susmita.parkicompanion.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PRESCRIPTION_URIS = "extra_prescription_uris";

    private ViewPager2 viewPager;
    private TextView tvPageIndicator;
    private Button btnPrevImage;
    private Button btnNextImage;

    private final List<String> uriList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription_viewer);

        Button btnViewerBack = findViewById(R.id.btnViewerBack);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
        viewPager = findViewById(R.id.viewPagerPrescriptions);
        btnPrevImage = findViewById(R.id.btnPrevImage);
        btnNextImage = findViewById(R.id.btnNextImage);

        btnViewerBack.setOnClickListener(v -> finish());

        String rawUris = getIntent().getStringExtra(EXTRA_PRESCRIPTION_URIS);
        if (rawUris != null && !rawUris.trim().isEmpty()) {
            String[] split = rawUris.split("\\|");
            for (String s : split) {
                if (!s.trim().isEmpty()) {
                    uriList.add(s.trim());
                }
            }
        }

        if (uriList.isEmpty()) {
            Toast.makeText(this, "No prescription document available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PrescriptionPagerAdapter adapter = new PrescriptionPagerAdapter(uriList);
        viewPager.setAdapter(adapter);

        updatePageIndicator(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePageIndicator(position);
            }
        });

        btnPrevImage.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) {
                viewPager.setCurrentItem(current - 1);
            }
        });

        btnNextImage.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < uriList.size() - 1) {
                viewPager.setCurrentItem(current + 1);
            }
        });
    }

    private void updatePageIndicator(int position) {
        tvPageIndicator.setText((position + 1) + " / " + uriList.size());
        btnPrevImage.setEnabled(position > 0);
        btnNextImage.setEnabled(position < uriList.size() - 1);
    }

    private static class PrescriptionPagerAdapter extends RecyclerView.Adapter<PrescriptionPagerAdapter.PageViewHolder> {

        private final List<String> items;

        public PrescriptionPagerAdapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivPage;
            private final View layoutPdfContainer;
            private final TextView tvPdfFileName;
            private final Button btnOpenPdfExternal;

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPage = itemView.findViewById(R.id.ivPrescriptionPage);
                layoutPdfContainer = itemView.findViewById(R.id.layoutPdfContainer);
                tvPdfFileName = itemView.findViewById(R.id.tvPdfFileName);
                btnOpenPdfExternal = itemView.findViewById(R.id.btnOpenPdfExternal);
            }

            public void bind(String uriStr) {
                boolean isPdf = uriStr.toLowerCase().contains(".pdf") || uriStr.toLowerCase().contains("pdf");

                if (isPdf) {
                    boolean renderedInApp = renderPdfToImageView(uriStr);
                    if (renderedInApp) {
                        layoutPdfContainer.setVisibility(View.GONE);
                        ivPage.setVisibility(View.VISIBLE);
                    } else {
                        ivPage.setVisibility(View.GONE);
                        layoutPdfContainer.setVisibility(View.VISIBLE);
                        tvPdfFileName.setText("PDF Document: " + getFileNameFromPath(uriStr));
                    }

                    btnOpenPdfExternal.setOnClickListener(v -> openPdfFileExternal(v, uriStr));
                } else {
                    layoutPdfContainer.setVisibility(View.GONE);
                    ivPage.setVisibility(View.VISIBLE);

                    try {
                        Uri uri;
                        if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                            uri = Uri.parse(uriStr);
                        } else {
                            uri = Uri.fromFile(new File(uriStr));
                        }
                        ivPage.setImageURI(uri);
                    } catch (Exception e) {
                        Toast.makeText(itemView.getContext(), "Could not load prescription image", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            private boolean renderPdfToImageView(String pathStr) {
                Context context = itemView.getContext();
                ParcelFileDescriptor pfd = null;
                PdfRenderer renderer = null;
                PdfRenderer.Page page = null;
                try {
                    if (pathStr.startsWith("content://")) {
                        Uri uri = Uri.parse(pathStr);
                        pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                    } else {
                        File pdfFile;
                        if (pathStr.startsWith("file://")) {
                            String path = Uri.parse(pathStr).getPath();
                            pdfFile = new File(path != null ? path : pathStr);
                        } else {
                            pdfFile = new File(pathStr);
                        }
                        if (!pdfFile.exists()) {
                            return false;
                        }
                        pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                    }

                    if (pfd == null) {
                        return false;
                    }

                    renderer = new PdfRenderer(pfd);
                    if (renderer.getPageCount() > 0) {
                        page = renderer.openPage(0);
                        int pageWidth = page.getWidth();
                        int pageHeight = page.getHeight();

                        float scale = Math.min(2.0f, 2048.0f / Math.max(pageWidth, pageHeight));
                        int renderWidth = Math.max(1, Math.round(pageWidth * scale));
                        int renderHeight = Math.max(1, Math.round(pageHeight * scale));

                        Bitmap bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        ivPage.setImageBitmap(bitmap);
                        return true;
                    }
                } catch (Throwable t) {
                    Log.e("PrescriptionViewer", "Failed to render PDF in-app", t);
                } finally {
                    try {
                        if (page != null) page.close();
                        if (renderer != null) renderer.close();
                        if (pfd != null) pfd.close();
                    } catch (Exception ignored) {
                    }
                }
                return false;
            }

            private void openPdfFileExternal(View v, String uriStr) {
                Context context = v.getContext();
                try {
                    Uri contentUri;
                    if (uriStr.startsWith("content://")) {
                        contentUri = Uri.parse(uriStr);
                    } else {
                        File file;
                        if (uriStr.startsWith("file://")) {
                            String filePath = Uri.parse(uriStr).getPath();
                            file = new File(filePath != null ? filePath : uriStr);
                        } else {
                            file = new File(uriStr);
                        }
                        if (!file.exists()) {
                            Toast.makeText(context, "Prescription file not found on device.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        contentUri = FileProvider.getUriForFile(
                                context,
                                context.getPackageName() + ".fileprovider",
                                file
                        );
                    }

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(contentUri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    PackageManager pm = context.getPackageManager();
                    List<ResolveInfo> resInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                    Intent chooser = Intent.createChooser(intent, "Open PDF with...");
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    context.startActivity(chooser);
                } catch (ActivityNotFoundException e) {
                    Log.e("PrescriptionViewer", "No PDF viewer app found", e);
                    Toast.makeText(context, "Cannot open PDF. Please install a PDF viewer app.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("PrescriptionViewer", "Error opening PDF", e);
                    Toast.makeText(context, "Cannot open PDF: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            private String getFileNameFromPath(String path) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1 && lastSlash < path.length() - 1) {
                    return path.substring(lastSlash + 1);
                }
                return path;
            }
        }
    }
}