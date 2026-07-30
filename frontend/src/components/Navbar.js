import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  Avatar,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  Description as DocIcon,
  Logout as LogoutIcon,
  Search as SearchIcon,
  DarkMode as DarkModeIcon,
  LightMode as LightModeIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { useThemeMode } from '../context/ThemeContext';

const Navbar = () => {
  const { user, logout } = useAuth();
  const { mode, toggleTheme } = useThemeMode();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <AppBar position="static" elevation={1}>
      <Toolbar>
        <DocIcon sx={{ mr: 1 }} />
        <Typography
          variant="h6"
          sx={{ cursor: 'pointer', fontWeight: 600 }}
          onClick={() => navigate('/dashboard')}
        >
          CollabEditor
        </Typography>

        <Box sx={{ flexGrow: 1 }} />

        <Tooltip title={mode === 'dark' ? 'Light mode' : 'Dark mode'}>
          <IconButton color="inherit" onClick={toggleTheme} size="small" sx={{ mr: 1 }}>
            {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
          </IconButton>
        </Tooltip>

        {user && (
          <Box display="flex" alignItems="center" gap={2}>
            <Button
              color="inherit"
              size="small"
              startIcon={<SearchIcon />}
              onClick={() => navigate('/search')}
            >
              Search
            </Button>
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main', fontSize: 14 }}>
              {user.name?.charAt(0)?.toUpperCase()}
            </Avatar>
            <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' } }}>
              {user.name}
            </Typography>
            <Button
              color="inherit"
              size="small"
              startIcon={<LogoutIcon />}
              onClick={handleLogout}
            >
              Logout
            </Button>
          </Box>
        )}
      </Toolbar>
    </AppBar>
  );
};

export default Navbar;
