"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";

interface ServerUrlPromptProps {
  initialUrl?: string;
  onSubmit: (url: string) => void;
}

export function ServerUrlPrompt({ initialUrl = "", onSubmit }: ServerUrlPromptProps) {
  const [url, setUrl] = useState(initialUrl);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (url.trim()) onSubmit(url.trim());
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>vLLM Remote</CardTitle>
          <CardDescription>
            Enter the URL of your vLLM management server to get started.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-3">
            <Input
              type="url"
              placeholder="http://100.x.x.x:9090"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              autoFocus
            />
            <Button type="submit" disabled={!url.trim()}>
              Connect
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
