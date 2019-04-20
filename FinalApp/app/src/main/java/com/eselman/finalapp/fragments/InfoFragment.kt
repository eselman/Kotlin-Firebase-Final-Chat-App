package com.eselman.finalapp.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.eselman.finalapp.R
import com.eselman.finalapp.models.TotalMessagesEvent
import com.eselman.finalapp.toast
import com.eselman.finalapp.utils.CircleTransform
import com.eselman.finalapp.utils.RxBus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.squareup.picasso.Picasso
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_info.view.*
import java.util.EventListener

class InfoFragment : Fragment() {
    private lateinit var _view: View

    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var currentUser: FirebaseUser

    private val store = FirebaseFirestore.getInstance()
    private lateinit var chatDBRef: CollectionReference

    private var chatSubscription: ListenerRegistration? = null
    private lateinit var infoBusListener: Disposable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _view = inflater.inflate(R.layout.fragment_info, container, false)

        setupChatDB()
        setupCurrentUser()
        setupCurrentUserInfoUI()

        // Total Messages Firebase Style
        //subscribeToTotalMessagesFirebaseStyle()

        // Total Messages Event Bus + Reactive Style
        subscribeToTotalMessagesEventBusReactiveStyle()

        return _view
    }

    override fun onDestroyView() {
        infoBusListener.dispose()
        chatSubscription?.remove()
        super.onDestroyView()
    }

    private fun setupChatDB() {
        chatDBRef = store.collection("chat")

    }

    private fun setupCurrentUser() {
        currentUser = mAuth.currentUser!!
    }

    private fun setupCurrentUserInfoUI() {
        _view.textViewInfoEmail.text = currentUser.email
        _view.textViewInfoName.text = currentUser.displayName?.let { currentUser.displayName } ?: run { getString(R.string.info_no_name) }
        currentUser.photoUrl?.let {
            Picasso.get().load(currentUser.photoUrl).resize(300,300)
                .centerCrop().transform(CircleTransform()).into(_view.imageViewInfoAvatar)
        } ?: run {
            Picasso.get().load(R.drawable.ic_person).resize(300,300)
                .centerCrop().transform(CircleTransform()).into(_view.imageViewInfoAvatar)
        }

    }

    private fun subscribeToTotalMessagesFirebaseStyle() {
        chatSubscription = chatDBRef.addSnapshotListener(object: EventListener, com.google.firebase.firestore.EventListener<QuerySnapshot>{
            override fun onEvent(querySnapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {
                exception?.let {
                    activity!!.toast("Exception!")
                    return
                }

                querySnapshot?.let {
                    _view.textViewInfoTotalMessages.text = "${it.size()}"
                }
            }

        })
    }

    private fun subscribeToTotalMessagesEventBusReactiveStyle() {
        infoBusListener = RxBus.listen(TotalMessagesEvent::class.java).subscribe{
          _view.textViewInfoTotalMessages.text = "${it.total}"
        }
    }
}
