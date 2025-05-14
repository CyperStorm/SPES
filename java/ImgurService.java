package com.example.colorsystem;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ImgurService {
    @Multipart
    @POST("3/image")
    @Headers("Authorization: Client-ID 87679ef011a8b28") // Replace YOUR_CLIENT_ID with your actual Imgur Client ID
    Call<ResponseBody> uploadImage(@Part MultipartBody.Part image);
}
