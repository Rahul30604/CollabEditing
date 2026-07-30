import React, { useState, useEffect, useRef } from 'react';
import { Box, Paper, Typography, Fade } from '@mui/material';
import { SmartToy as BotIcon } from '@mui/icons-material';

const INITIAL_MESSAGE = `Welcome to CollabEditor!

To edit this document, request the editing key by clicking "Request Key" on the top right corner.`;

const REMINDER_MESSAGE = `If you want to edit the document, press "Request Key" on the top right corner.`;

const ONBOARDING_TIPS = `Great! You've requested the key. Here are some things you can do:

1. Edit the document - Type anything, changes are saved with Cmd/Ctrl+S
2. Version History - Click the clock icon to see all saved versions
3. Compare Versions - View differences between any two versions
4. AI Assistant - Click the sparkle icon to ask questions about the document
5. Summarize - Get an AI-generated summary of the document
6. Semantic Search - Use the Search button in the navbar to find content across all documents
7. Share - Click "+ Add People" to invite collaborators by email

Passing the key to you now... Happy editing!`;

const TutorialBot = ({ isActive, onKeyRequested, onComplete }) => {
  const [messages, setMessages] = useState([]);
  const [showReminder, setShowReminder] = useState(false);
  const [phase, setPhase] = useState('waiting'); // waiting, onboarding, done
  const reminderRef = useRef(null);
  const messagesEndRef = useRef(null);

  // Show initial message
  useEffect(() => {
    if (isActive) {
      setMessages([{ text: INITIAL_MESSAGE, type: 'bot' }]);

      // Start reminder interval
      reminderRef.current = setInterval(() => {
        setShowReminder(true);
        setMessages((prev) => {
          // Don't add duplicate reminders
          const lastMsg = prev[prev.length - 1];
          if (lastMsg && lastMsg.text === REMINDER_MESSAGE) return prev;
          return [...prev, { text: REMINDER_MESSAGE, type: 'reminder' }];
        });
      }, 20000);

      return () => {
        if (reminderRef.current) clearInterval(reminderRef.current);
      };
    }
  }, [isActive]);

  // Scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Called when user requests the key
  useEffect(() => {
    if (onKeyRequested && phase === 'waiting') {
      setPhase('onboarding');

      // Clear reminders
      if (reminderRef.current) clearInterval(reminderRef.current);

      // Show onboarding tips
      setMessages((prev) => [...prev, { text: ONBOARDING_TIPS, type: 'bot' }]);

      // After 5 seconds, pass the key
      setTimeout(() => {
        setPhase('done');
        if (onComplete) onComplete();
      }, 5000);
    }
  }, [onKeyRequested, phase, onComplete]);

  if (!isActive || phase === 'done') return null;

  return (
    <Fade in={isActive}>
      <Paper
        elevation={3}
        sx={{
          position: 'fixed',
          bottom: 24,
          right: 24,
          width: 340,
          maxHeight: 400,
          overflow: 'auto',
          p: 2,
          bgcolor: '#f0f7ff',
          border: '1px solid #1976d2',
          borderRadius: 2,
          zIndex: 1100,
        }}
      >
        <Box display="flex" alignItems="center" gap={1} mb={1}>
          <BotIcon color="primary" fontSize="small" />
          <Typography variant="subtitle2" fontWeight={600} color="primary">
            CollabEditor Bot
          </Typography>
        </Box>

        {messages.map((msg, idx) => (
          <Typography
            key={idx}
            variant="body2"
            sx={{
              whiteSpace: 'pre-wrap',
              mb: 1,
              p: 1,
              borderRadius: 1,
              bgcolor: msg.type === 'reminder' ? '#fff3e0' : '#ffffff',
              border: msg.type === 'reminder' ? '1px solid #ff9800' : '1px solid #e0e0e0',
              fontSize: '0.8rem',
              lineHeight: 1.5,
            }}
          >
            {msg.text}
          </Typography>
        ))}
        <div ref={messagesEndRef} />
      </Paper>
    </Fade>
  );
};

export default TutorialBot;
