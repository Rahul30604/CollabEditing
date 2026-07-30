#!/bin/sh
# Replace placeholder with actual API URL at runtime
if [ -n "$REACT_APP_API_URL" ]; then
  find /usr/share/nginx/html -name '*.js' -exec sed -i "s|__REACT_APP_API_URL__|${REACT_APP_API_URL}|g" {} +
fi

exec "$@"
