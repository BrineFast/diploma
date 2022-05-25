package com.diploma.slepov.custom

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.util.concurrent.Executor

fun <TResult> Task<TResult>.addOnSuccessListener(executor: Executor,
                                                 listener: (TResult) -> Unit): Task<TResult> {
    return addOnSuccessListener(executor, OnSuccessListener(listener))
}

fun <TResult> Task<TResult>.addOnFailureListener(executor: Executor,
                                                 listener: (Exception) -> Unit): Task<TResult> {
    return addOnFailureListener(executor, OnFailureListener(listener))
}

fun <TResult> Task<TResult>.addOnCompleteListener(executor: Executor,
                                                  listener: (Task<TResult>) -> Unit): Task<TResult> {
    return addOnCompleteListener(executor, OnCompleteListener(listener))
}

fun <TResult> Task<TResult>.addOnCanceledListener(executor: Executor,
                                                  listener: () -> Unit): Task<TResult> {
    return addOnCanceledListener(executor, OnCanceledListener(listener))
}
