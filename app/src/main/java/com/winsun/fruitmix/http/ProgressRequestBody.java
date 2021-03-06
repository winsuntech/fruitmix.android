package com.winsun.fruitmix.http;

import android.util.Log;

import com.winsun.fruitmix.file.data.upload.FileUploadFinishedState;
import com.winsun.fruitmix.file.data.upload.FileUploadItem;
import com.winsun.fruitmix.file.data.upload.FileUploadState;
import com.winsun.fruitmix.file.data.upload.FileUploadingState;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by Administrator on 2017/11/15.
 */

public class ProgressRequestBody extends RequestBody {

    public static final String TAG = ProgressRequestBody.class.getSimpleName();

    private RequestBody requestBody;
    private BufferedSink bufferedSink;
    private FileUploadState newFileUploadState;

    private FileUploadItem fileUploadItem;

    public ProgressRequestBody(RequestBody requestBody, FileUploadState fileUploadState) {
        this.requestBody = requestBody;

        fileUploadItem = fileUploadState.getFileUploadItem();

        newFileUploadState = new FileUploadingState(fileUploadItem);

        fileUploadItem.setFileUploadState(newFileUploadState);

    }

    /**
     * Returns the Content-Type header for this body.
     */
    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    /**
     * Writes the content of this request to {@code out}.
     *
     * @param sink
     */
    @Override
    public void writeTo(BufferedSink sink) throws IOException {

        if (bufferedSink == null)
            bufferedSink = Okio.buffer(sink(sink));

        requestBody.writeTo(bufferedSink);
        bufferedSink.flush();

    }

    private Sink sink(BufferedSink sink) {

        return new ForwardingSink(sink) {
            long bytesWritten = 0L;
            long contentLength = 0L;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    contentLength = contentLength();

                    Log.d(TAG, "write: contentLength:" + contentLength);
                }
                bytesWritten += byteCount;

                if (bytesWritten == contentLength) {

                    Log.d(TAG, "currentUploadSize: " + bytesWritten);

                    newFileUploadState.setFileCurrentTaskSize(bytesWritten);

                    newFileUploadState.notifyDownloadStateChanged();

                } else {

                    if (bytesWritten - newFileUploadState.getFileUploadItem().getFileCurrentTaskSize() > 1024) {

                        newFileUploadState.setFileCurrentTaskSize(bytesWritten);

                        newFileUploadState.notifyDownloadStateChanged();

                    }

                }

            }
        };
    }

}
