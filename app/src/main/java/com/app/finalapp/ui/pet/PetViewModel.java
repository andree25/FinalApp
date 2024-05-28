package com.app.finalapp.ui.pet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class PetViewModel extends ViewModel {
    private final MutableLiveData<List<Uri>> imagesUriList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> uploadedUrls = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<Uri>> getImagesUriList() {
        return imagesUriList;
    }

    public LiveData<List<String>> getUploadedUrls() {
        return uploadedUrls;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void addImageUri(Uri uri) {
        List<Uri> currentList = new ArrayList<>(imagesUriList.getValue());
        currentList.add(uri);
        imagesUriList.setValue(currentList);
    }

    public void removeImageUri(int position) {
        List<Uri> currentList = new ArrayList<>(imagesUriList.getValue());
        if (position >= 0 && position < currentList.size()) {
            currentList.remove(position);
            imagesUriList.setValue(currentList);
        }
    }

    public void addUploadedUrl(String url) {
        List<String> currentList = new ArrayList<>(uploadedUrls.getValue());
        currentList.add(url);
        uploadedUrls.setValue(currentList);
    }

    public void setLoading(boolean loading) {
        isLoading.setValue(loading);
    }

    public void clearUploadedUrls() {
        uploadedUrls.setValue(new ArrayList<>());
    }
}
