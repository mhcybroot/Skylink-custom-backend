self.addEventListener('push', function(event) {
  let data = {};
  if (event.data) {
    try {
        data = event.data.json();
    } catch(e) {
        data = { title: 'Skylink Notification', body: event.data.text(), url: '/notifications/history' };
    }
  } else {
      data = { title: 'Skylink Notification', body: 'You have a new update.', url: '/notifications/history' };
  }

  const options = {
    body: data.body,
    icon: '/images/skylink-logo.png', // Replace with valid path if available
    badge: '/images/skylink-badge.png', // Small monochrome icon
    data: {
      url: data.url
    },
    requireInteraction: true // Keeps notification visible until clicked
  };

  event.waitUntil(
    self.registration.showNotification(data.title, options)
  );
});

self.addEventListener('notificationclick', function(event) {
  event.notification.close();
  
  if (event.notification.data && event.notification.data.url) {
      event.waitUntil(
        clients.openWindow(event.notification.data.url)
      );
  } else {
      event.waitUntil(
        clients.openWindow('/notifications/history')
      );
  }
});
