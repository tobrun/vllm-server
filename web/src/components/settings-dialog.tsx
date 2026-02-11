"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Settings } from "lucide-react";

interface SettingsDialogProps {
  currentUrl: string;
  onSave: (url: string) => void;
}

export function SettingsDialog({ currentUrl, onSave }: SettingsDialogProps) {
  const [open, setOpen] = useState(false);
  const [url, setUrl] = useState(currentUrl);

  const handleOpen = (isOpen: boolean) => {
    setOpen(isOpen);
    if (isOpen) setUrl(currentUrl);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="icon">
          <Settings className="h-5 w-5" />
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Server Settings</DialogTitle>
        </DialogHeader>
        <Input
          type="url"
          placeholder="http://100.x.x.x:9090"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
        />
        <DialogFooter>
          <Button variant="secondary" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button
            onClick={() => {
              if (url.trim()) {
                onSave(url.trim());
                setOpen(false);
              }
            }}
          >
            Save
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
