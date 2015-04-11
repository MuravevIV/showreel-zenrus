package com.ilyamur.showreel.zenrus.http

import com.ilyamur.bixbite.http.client.HttpConnectionManagerImpl

class HttpConnectionManagerZenrus extends HttpConnectionManagerImpl(
    optProxy = None,
    optTruststore = None,
    timeoutMs = 20000
)