"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Power } from "lucide-react";

interface ShutdownSectionProps {
  isLoading: boolean;
  onShutdown: () => void;
}

export function ShutdownSection({ isLoading, onShutdown }: ShutdownSectionProps) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <Button
        variant="destructive"
        onClick={() => setOpen(true)}
        disabled={isLoading}
        className="w-full"
      >
        <Power className="h-4 w-4 mr-2" />
        Shutdown Server
      </Button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Shutdown Server?</DialogTitle>
            <DialogDescription>
              This will shut down the server machine. You will need physical access or
              another remote method to start it again.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={() => {
                setOpen(false);
                onShutdown();
              }}
            >
              Shut Down
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
