package com.eselman.finalapp.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.eselman.finalapp.R
import com.eselman.finalapp.adapters.ChatAdapter
import com.eselman.finalapp.models.Message
import com.eselman.finalapp.models.TotalMessagesEvent
import com.eselman.finalapp.toast
import com.eselman.finalapp.utils.RxBus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.fragment_chat.*
import kotlinx.android.synthetic.main.fragment_chat.view.*
import java.util.*
import java.util.EventListener
import kotlin.collections.HashMap

class ChatFragment : Fragment() {
    private lateinit var _view: View

    private lateinit var adapter: ChatAdapter
    private val messageList: ArrayList<Message> = ArrayList()

    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var currentUser: FirebaseUser

    private val store = FirebaseFirestore.getInstance()
    private lateinit var chatDBRef: CollectionReference

    private var chatSubscription: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _view = inflater.inflate(R.layout.fragment_chat, container, false)

        setupChatDB()
        setupCurrentUser()
        setupRecyclerView()
        setupChatBtn()

        subscribeToChatMessages()

        return _view
    }

    override fun onDestroyView() {
        chatSubscription?.remove()
        super.onDestroyView()
    }

    private fun setupChatDB() {
        chatDBRef = store.collection("chat")

    }

    private fun setupCurrentUser() {
        currentUser = mAuth.currentUser!!
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        adapter = ChatAdapter(messageList, currentUser.uid)

        _view.recyclerView.setHasFixedSize(true)
        _view.recyclerView.layoutManager = layoutManager
        _view.recyclerView.itemAnimator = DefaultItemAnimator()
        _view.recyclerView.adapter = adapter
    }

    private fun setupChatBtn() {
        _view.buttonSend.setOnClickListener {
            val messageText = editTextMessage.text.toString()
            if(messageText.isNotEmpty()) {
                val photo = currentUser.photoUrl?.let { currentUser.photoUrl.toString() } ?: run {""}
                val message = Message(currentUser.uid, messageText, photo, Date())
                saveMessage(message)

                _view.editTextMessage.setText("")
            }
        }
    }

    private fun saveMessage(message: Message) {
        val newMessage = HashMap<String, Any>()

        newMessage["authorId"] = message.authorId
        newMessage["message"] = message.message
        newMessage["profileImageURL"] = message.profileImageURL
        newMessage["sentAt"] = message.sentAt

        chatDBRef.add(newMessage)
            .addOnCompleteListener {
                activity!!.toast("Message Added")
            }
            .addOnFailureListener {
                activity!!.toast("Message error, try again!")
            }
    }

    private fun subscribeToChatMessages() {
        chatSubscription = chatDBRef
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener(object: EventListener, com.google.firebase.firestore.EventListener<QuerySnapshot>{
            override fun onEvent(snapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {
                exception?.let {
                    activity!!.toast("Exception!")
                    return
                }

                snapshot?.let {
                    messageList.clear()
                    val messages = it.toObjects(Message::class.java)
                    messageList.addAll(messages.asReversed())
                    adapter.notifyDataSetChanged()
                    _view.recyclerView.smoothScrollToPosition(messageList.size)
                    RxBus.publish(TotalMessagesEvent(messageList.size))
                }
            }

        })


    }
}
