package com.example.contactapp.Model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: Uri?
) : Parcelable
